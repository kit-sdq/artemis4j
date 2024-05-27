package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import edu.kit.kastel.sdq.artemis4j.metajson.MetaFeedbackMapper;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.MistakeType;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.Points;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.RatingGroup;
import edu.kit.kastel.sdq.artemis4j.new_client.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.new_client.FeedbackSplitter;
import org.slf4j.Logger;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Assessment extends ArtemisClientHolder {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Assessment.class);

    private static final FormatString MANUAL_FEEDBACK = new FormatString(new MessageFormat("[{0}:{1}] {2}"));
    private static final FormatString MANUAL_FEEDBACK_CUSTOM = new FormatString(new MessageFormat("[{0}:{1}] {2}\nExplanation: {3}"));
    private static final FormatString GLOBAL_FEEDBACK_HEADER = new FormatString(new MessageFormat("{0} [{1,number,##.###} (Range: {2,number,##.###} -- {3,number,##.###}) points]"));
    private static final FormatString GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER = new FormatString(new MessageFormat("    * \"{0}\" [{1,number,##.###}P]:"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION = new FormatString(new MessageFormat("        * {0} at line {1,number}"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY = new FormatString(new MessageFormat("        * {0} at line {1,number} ({0,number,##.###}P)"));
    private static final FormatString GLOBAL_FEEDBACK_LIMIT_OVERRUN = new FormatString(new MessageFormat("    * Note: The sum of penalties hit the limits for this rating group."));

    private final List<Annotation> annotations;
    private final List<TestResult> testResults;
    private final Submission submission;
    private final GradingConfig config;

    public Assessment(ResultDTO result, GradingConfig config, Submission submission) throws AnnotationMappingException {
        super(submission);
        this.submission = submission;
        this.config = config;

        // Unpack the result
        this.annotations = MetaFeedbackMapper.parseMetaFeedbacks(result.feedbacks(), config);
        this.testResults = Arrays.stream(result.feedbacks()).filter(f -> f.type() == FeedbackType.AUTOMATIC).map(TestResult::new).toList();
    }

    public List<Annotation> getAnnotations() {
        return Collections.unmodifiableList(this.annotations);
    }

    public List<Annotation> getAnnotations(MistakeType mistakeType) {
        return this.annotations.stream().filter(a -> a.getMistakeType().equals(mistakeType)).toList();
    }

    public List<Annotation> getAnnotations(RatingGroup ratingGroup) {
        return this.annotations.stream().filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup)).toList();
    }

    public void addAnnotation(Annotation annotation) {
        this.annotations.add(annotation);
    }

    public void clearAnnotations() {
        this.annotations.clear();
    }

    public void saveOrSubmit(boolean submit, Locale artemisLocale) throws AnnotationMappingException, ArtemisNetworkException {
        log.info("Packing assessment for artemis");
        var feedbacks = this.packAssessmentForArtemis(artemisLocale);
        var absoluteScore = this.calculateTotalPointsIncludingTests();
        var relativeScore = absoluteScore / this.getMaxPoints() * 100.0;
        var result = ResultDTO.forAssessmentSubmission(this.submission.getId(), relativeScore, feedbacks.toArray(FeedbackDTO[]::new), this.getClient().getAssessor().toDTO());

        // Sanity check
        var feedbackPoints = Arrays.stream(result.feedbacks()).mapToDouble(FeedbackDTO::credits).sum();
        if (absoluteScore != feedbackPoints) {
            throw new IllegalStateException("Feedback points do not match the calculated points. Calculated " + absoluteScore + " but feedbacks sum up to " + feedbackPoints + " points.");
        }

        SubmissionDTO.saveAssessment(this.getClient(), this.submission.getParticipationId(), submit, result);
    }

    public void cancel() throws ArtemisNetworkException {
        SubmissionDTO.cancelAssessment(this.getClient(), this.submission.getId());
    }

    public double calculateTotalPointsIncludingTests() {
        var points = this.config.ratingGroups().stream()
                .map(this::calculatePointsForRatingGroup)
                .mapToDouble(Points::penalty)
                .sum();
        points += this.testResults.stream().mapToDouble(TestResult::getPoints).sum();
        return Math.clamp(points, 0.0, this.getMaxPoints());
    }

    public double getMaxPoints() {
        return this.submission.getExercise().getMaxPoints();
    }

    public Optional<Points> calculatePointsForMistakeType(MistakeType mistakeType) {
        var annotationsWithType = this.getAnnotations(mistakeType);
        if (annotationsWithType.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mistakeType.getRule().calculatePenalty(annotationsWithType));
    }

    public Points calculatePointsForRatingGroup(RatingGroup ratingGroup) {
        double points = this.annotations.stream()
                .filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup))
                .collect(Collectors.groupingBy(Annotation::getMistakeType))
                .entrySet()
                .stream()
                .mapToDouble(e -> e.getKey().getRule().calculatePenalty(e.getValue()).penalty())
                .sum();
        if (points > ratingGroup.getMaxPenalty()) {
            return new Points(ratingGroup.getMaxPenalty(), true);
        } else if (points < ratingGroup.getMinPenalty()) {
            return new Points(ratingGroup.getMinPenalty(), true);
        } else {
            return new Points(points, false);
        }
    }

    private List<FeedbackDTO> packAssessmentForArtemis(Locale locale) throws AnnotationMappingException {
        // We need all automatic feedback
        List<FeedbackDTO> feedbacks = new ArrayList<>(this.testResults.stream().map(TestResult::getDto).toList());

        // Add the meta feedback(s)
        feedbacks.addAll(MetaFeedbackMapper.createMetaFeedbacks(this.annotations));

        // For each annotation we have a manual feedback at the respective line
        // These feedbacks deduct no points. They are just for the student to see in the Artemis code viewer
        // We group annotations by file and line to have at most one annotation per line
        feedbacks.addAll(this.annotations.stream()
                .collect(
                        Collectors.groupingBy(Annotation::getFilePath,
                                Collectors.groupingBy(Annotation::getStartLine)))
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(e -> createInlineFeedback(e, locale))
                .toList()
        );

        // We have on (or more if they are too long) global feedback per rating group
        // These feedbacks deduct points
        feedbacks.addAll(this.config.ratingGroups().stream().flatMap(r -> createGlobalFeedback(r, locale).stream()).toList());

        log.info("Created {} manual feedbacks for artemis", feedbacks.stream().filter(f -> f.type() == FeedbackType.MANUAL).count());
        log.info("Created {} manual-unreferenced feedbacks for artemis", feedbacks.stream().filter(f -> f.type() == FeedbackType.MANUAL_UNREFERENCED).count());

        return feedbacks;
    }

    private FeedbackDTO createInlineFeedback(Map.Entry<Integer, List<Annotation>> annotations, Locale locale) {
        var sampleAnnotation = annotations.getValue().getFirst();

        String text = "File " + sampleAnnotation.getFilePathWithoutType() + " at line " + sampleAnnotation.getDisplayLine();
        String reference = "file:" + sampleAnnotation.getFilePath() + "_line:" + sampleAnnotation.getStartLine();
        String detailText = annotations.getValue().stream()
                .map(a -> {
                    if (a.getCustomMessage().isPresent()) {
                        return MANUAL_FEEDBACK_CUSTOM.format(
                                a.getMistakeType().getRatingGroup().getDisplayName(),
                                a.getMistakeType().getButtonText(),
                                a.formatMessageForArtemis(),
                                a.getCustomMessage().orElseThrow()).translateTo(locale);
                    } else {
                        return MANUAL_FEEDBACK.format(
                                a.getMistakeType().getRatingGroup().getDisplayName(),
                                a.getMistakeType().getButtonText(),
                                a.formatMessageForArtemis()).translateTo(locale);
                    }
                })
                .collect(Collectors.joining("\n\n")).trim();
        return FeedbackDTO.newManual(0.0, text, reference, detailText);
    }

    /**
     * This builds one (or more if the feedback is too long) global feedback for a rating group.
     * The feedback deducts points, and lists all annotations that are part of the rating group.
     * @param ratingGroup
     * @param locale
     * @return
     */
    private List<FeedbackDTO> createGlobalFeedback(RatingGroup ratingGroup, Locale locale) {
        var points = this.calculatePointsForRatingGroup(ratingGroup);

        // Header:
        // Methodik [-1 (Range: -4 -- 0) points]
        // The header is reused for every sub-feedback
        var header = GLOBAL_FEEDBACK_HEADER.format(ratingGroup.getDisplayName(), points.penalty(), ratingGroup.getMinPenalty(), ratingGroup.getMaxPenalty());

        // First collect only the lines so that we can later split the feedback by lines
        List<TranslatableString> lines = new ArrayList<>();
        for (var mistakeType : ratingGroup.getMistakeTypes()) {
            var score = this.calculatePointsForMistakeType(mistakeType);
            if (score.isPresent()) {
                lines.add(GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER.format(mistakeType.getButtonText(), score.get().penalty()));
                for (var annotation : this.getAnnotations(mistakeType)) {
                    // For custom annotations, we have '* <file> at line <line> (<penalty>P)'
                    // Otherwise, it's just '* <file> at line <line>'
                    // Lines are zero-indexed
                    if (annotation.getCustomPenalty().isPresent()) {
                        lines.add(GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY.format(annotation.getFilePath(), annotation.getDisplayLine(), annotation.getCustomPenalty().get()));
                    } else {
                        lines.add(GLOBAL_FEEDBACK_ANNOTATION.format(annotation.getFilePath(), annotation.getDisplayLine()));
                    }
                }
            }
        }

        // No feedback, so return early
        if (lines.isEmpty()) {
            return List.of();
        }

        // Add a remark if we hit the limits
        if (points.limitOverrun()) {
            lines.add(GLOBAL_FEEDBACK_LIMIT_OVERRUN.format());
        }

        // Possibly split into multiple feedbacks
        List<String> feedbackTexts = FeedbackSplitter.splitLines(lines, header, locale);
        if (feedbackTexts.size() == 1) {
            return List.of(FeedbackDTO.newVisibleManualUnreferenced(0.0, null, feedbackTexts.getFirst()));
        } else {
            // We have more than one feedback to create
            // To make it easier for students, each feedback gets a running index (annotation 1/2, annotation 2/2)
            // appended to its header
            List<FeedbackDTO> feedbacks = new ArrayList<>();
            for (int i = 0; i < feedbackTexts.size(); i++) {
                // Only the first feedback deducts points
                feedbacks.add(FeedbackDTO.newVisibleManualUnreferenced(i == 0 ? points.penalty() : 0.0, null, feedbackTexts.get(i)));
            }
            return feedbacks;
        }
    }
}
