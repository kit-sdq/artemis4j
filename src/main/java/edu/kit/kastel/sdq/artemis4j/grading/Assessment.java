/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.location.ComparatorUtils;
import edu.kit.kastel.sdq.artemis4j.grading.location.Location;
import edu.kit.kastel.sdq.artemis4j.grading.location.LocationFormatter;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.MetaFeedbackMapper;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.Points;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.RatingGroup;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import org.slf4j.Logger;

/**
 * An active assessment of a submission for which we hold a lock. This class
 * stores annotations, calculates points, and parses &amp; serializes feedbacks
 * for Artemis.
 */
public class Assessment extends ArtemisConnectionHolder {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(Assessment.class);

    private static final int DEFAULT_ANNOTATION_LIMIT = 12;
    private static final FormatString MANUAL_FEEDBACK = new FormatString(new MessageFormat("[{0}:{1}] {2}"));
    private static final FormatString MANUAL_FEEDBACK_CUSTOM_EXP =
            new FormatString(new MessageFormat("[{0}:{1}] " + "{2}\nExplanation: {3}"));
    private static final FormatString MANUAL_FEEDBACK_CUSTOM_PENALTY =
            new FormatString(new MessageFormat("[{0}:{1}] " + "{2} ({3,number,##.###}P)"));
    private static final FormatString GLOBAL_FEEDBACK_HEADER = new FormatString(
            new MessageFormat("{0} [{1,number,##" + ".###}P (Range: {2,number,##.###}P -- {3,number,##.###}P)]"));
    private static final FormatString GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER =
            new FormatString(new MessageFormat("    *" + " {0} [{1,number,##.###}P]:"));
    private static final FormatString GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER_NONSCORING =
            new FormatString(new MessageFormat("    * {0}:"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION =
            new FormatString(new MessageFormat("        * {0} at line {1}"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION_MULTIPLE =
            new FormatString(new MessageFormat("        * {0} at lines {1}"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY =
            new FormatString(new MessageFormat("        * {0} at line {1} ({2,number,##.###}P)"));
    private static final FormatString GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY_MULTIPLE =
            new FormatString(new MessageFormat("        * {0} at lines {1} ({2,number,##.###}P)"));
    private static final FormatString GLOBAL_FEEDBACK_LIMIT_OVERRUN = new FormatString(
            new MessageFormat("    * Note:" + " The sum of penalties hit the limits for this rating group."));
    private static final FormatString NO_FEEDBACK_DUMMY = new FormatString("The tutor has made no annotations.");

    /**
     * The global feedback containing a list of all annotations that were made, might be too large. In this case,
     * the feedback is split into multiple parts. Artemis will display feedbacks that subtract points in a different
     * section than feedbacks that do not subtract points.
     * <p>
     * With only the first feedback deducting points, the rest would be displayed in the info section. To prevent this,
     * the other parts are passed a negative score that is as close to zero as possible.
     * <p>
     * This negative score is defined by the following constant.
     * <p>
     * Neither -0.0 nor setting the positive flag to false was accepted by artemis.
     */
    private static final double GLOBAL_FEEDBACK_OTHER_PARTS_SCORE = -Double.MIN_VALUE;

    private final ResultDTO lockingResult;
    private final List<Annotation> annotations;
    private final List<TestResult> testResults;
    private final ProgrammingSubmission programmingSubmission;
    private final GradingConfig config;
    private final CorrectionRound correctionRound;
    private final Locale studentLocale;

    public Assessment(
            ResultDTO result,
            GradingConfig config,
            ProgrammingSubmission programmingSubmission,
            CorrectionRound correctionRound)
            throws AnnotationMappingException, ArtemisNetworkException {
        this(result, config, programmingSubmission, correctionRound, Locale.GERMANY);
    }

    public Assessment(
            ResultDTO result,
            GradingConfig config,
            ProgrammingSubmission programmingSubmission,
            CorrectionRound correctionRound,
            Locale studentLocale)
            throws AnnotationMappingException, ArtemisNetworkException {
        super(programmingSubmission);
        this.lockingResult = result;
        this.programmingSubmission = programmingSubmission;
        this.config = config;
        this.correctionRound = correctionRound;
        this.studentLocale = studentLocale;

        // ensure that the feedbacks are fetched (some api endpoints do not return them)
        // and for long feedbacks, we need to fetch the detailed feedbacks
        var feedbacks = ResultDTO.fetchDetailedFeedbacks(
                this.getConnection().getClient(),
                result.id(),
                programmingSubmission.getParticipationId(),
                result.feedbacks());

        // Unpack the result
        this.annotations = Collections.synchronizedList(MetaFeedbackMapper.parseMetaFeedbacks(feedbacks, config));
        this.testResults = feedbacks.stream()
                .filter(f -> f.type() == FeedbackType.AUTOMATIC)
                .map(TestResult::new)
                .toList();
    }

    /**
     * Get the submission associated with this assessment
     */
    public ProgrammingSubmission getSubmission() {
        return programmingSubmission;
    }

    /**
     * Get all annotations, including ones that were deleted in review. Use the add/remove methods to modify
     * the list of annotations.
     *
     * @return An unmodifiable list of annotations.
     */
    public List<Annotation> getAllAnnotations() {
        return Collections.unmodifiableList(this.annotations);
    }

    /**
     * Get the annotations that were not deleted in review. Use the add/remove methods to modify
     * the list of annotations.
     *
     * @return An unmodifiable list of annotations.
     */
    public List<Annotation> getNonDeletedAnnotations() {
        return this.annotations.stream().filter(a -> !a.isDeletedInReview()).toList();
    }

    /**
     * Stream the annotations that were not deleted in review.
     *
     * @return A stream of annotations.
     */
    public Stream<Annotation> streamNonDeletedAnnotations() {
        return this.annotations.stream().filter(a -> !a.isDeletedInReview());
    }

    /**
     * Gets all annotations associated with the specified mistake type. The mistake
     * type must be associated with the same grading config as this assessment.
     *
     * @return An unmodifiable list of annotations, possibly empty but never null.
     */
    public List<Annotation> getAnnotations(MistakeType mistakeType) {
        return this.getAnnotations(mistakeType, this.annotations);
    }

    private List<Annotation> getAnnotations(MistakeType mistakeType, Collection<Annotation> annotations) {
        return annotations.stream()
                .filter(a -> a.getMistakeType().equals(mistakeType))
                .toList();
    }

    /**
     * Gets all annotations associated with the specified rating group. The rating
     * group must be associated with the same grading config as this assessment.
     *
     * @return An unmodifiable list of annotations, possibly empty but never null.
     */
    public List<Annotation> getAnnotations(RatingGroup ratingGroup) {
        return this.annotations.stream()
                .filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup))
                .toList();
    }

    /**
     * Adds a non-custom manual annotation to the assessment.
     *
     * @param mistakeType   Must not be a custom mistake type
     * @param customMessage May be null if no custom message is provided
     */
    public Annotation addPredefinedAnnotation(
            MistakeType mistakeType, String filePath, int startLine, int endLine, String customMessage) {
        return this.addPredefinedAnnotation(mistakeType, new Location(filePath, startLine, endLine), customMessage);
    }

    /**
     * Adds a non-custom manual annotation to the assessment.
     *
     * @param mistakeType   Must not be a custom mistake type
     * @param customMessage May be null if no custom message is provided
     */
    public Annotation addPredefinedAnnotation(MistakeType mistakeType, Location location, String customMessage) {
        if (mistakeType.isCustomAnnotation()) {
            throw new IllegalArgumentException("Mistake type is a custom annotation");
        }

        var source = this.correctionRound.toAnnotationSource();
        var annotation = new Annotation(mistakeType, location, customMessage, null, source);
        this.annotations.add(annotation);
        return annotation;
    }

    /**
     * Adds a custom manual annotation to the assessment.
     *
     * @param mistakeType   Must be a custom mistake type
     * @param customMessage May not be null
     */
    public Annotation addCustomAnnotation(
            MistakeType mistakeType,
            String filePath,
            int startLine,
            int endLine,
            String customMessage,
            double customScore) {
        return this.addCustomAnnotation(
                mistakeType, new Location(filePath, startLine, endLine), customMessage, customScore);
    }

    /**
     * Adds a custom manual annotation to the assessment.
     *
     * @param mistakeType   Must be a custom mistake type
     * @param customMessage May not be null
     */
    public Annotation addCustomAnnotation(
            MistakeType mistakeType, Location location, String customMessage, double customScore) {
        if (!mistakeType.isCustomAnnotation()) {
            throw new IllegalArgumentException("Mistake type is not a custom annotation");
        }

        if (customScore > 0.0 && !this.config.isPositiveFeedbackAllowed()) {
            throw new IllegalArgumentException(
                    "Custom annotations with positive scores are not allowed for this exercise");
        }

        var source = this.correctionRound.toAnnotationSource();
        var annotation = new Annotation(mistakeType, location, customMessage, customScore, source);
        this.annotations.add(annotation);
        return annotation;
    }

    public Annotation addAutograderAnnotation(
            MistakeType mistakeType,
            Location location,
            String explanation,
            String checkName,
            String problemType,
            Integer annotationLimit) {
        Double customScore = mistakeType.isCustomAnnotation() ? 0.0 : null;
        var annotation = new Annotation(
                mistakeType,
                location,
                explanation,
                customScore,
                AnnotationSource.AUTOGRADER,
                List.of(checkName, problemType),
                annotationLimit);
        this.annotations.add(annotation);
        return annotation;
    }

    /**
     * Removes an annotation from the assessment. If the annotation is not present,
     * nothing happens.
     */
    public void removeAnnotation(Annotation annotation) {
        this.annotations.remove(annotation);
    }

    /**
     * Clears all annotations from the assessment.
     */
    public void clearAnnotations() {
        this.annotations.clear();
    }

    /**
     * Saves the assessment to Artemis. This does not free the lock on the
     * submission.
     */
    public void save() throws AnnotationMappingException, ArtemisNetworkException {
        this.internalSaveOrSubmit(false);
    }

    /**
     * Saves and submits the assessment. A submitted assessment can still be changed
     * if you have its ID, but it will be marked as assessed. This also frees the
     * lock on the submission.
     */
    public void submit() throws AnnotationMappingException, ArtemisNetworkException {
        this.internalSaveOrSubmit(true);
    }

    /**
     * Exports the assessment to a string. Useful for (offline) checkpointing of
     * assessments. The returned string contains all information necessary to
     * recreate the exact state of the assessment, as long as the underlying
     * submission is the same.
     */
    public String exportAssessment() throws AnnotationMappingException {
        String header = this.programmingSubmission.getId() + ";" + this.correctionRound.toArtemis() + ";";
        return header + MetaFeedbackMapper.serializeAnnotations(this.annotations);
    }

    /**
     * Imports the given assessment string. This overwrites the current assessment.
     * It is checked that the submission ID and correction round match the current
     * assessment.
     */
    public void importAssessment(String exportedAssessment) throws AnnotationMappingException {
        String[] parts = exportedAssessment.split(";", 3);
        try {
            if (Integer.parseInt(parts[0]) != this.programmingSubmission.getId()) {
                throw new IllegalArgumentException("Submission ID does not match");
            }
            if (Integer.parseInt(parts[1]) != this.correctionRound.toArtemis()) {
                throw new IllegalArgumentException("Correction round does not match");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid header in exported annotations");
        }

        this.annotations.clear();
        this.annotations.addAll(MetaFeedbackMapper.deserializeAnnotations(parts[2], this.config));
    }

    /**
     * Cancels the assessment &amp; frees the lock on the submission. This deletes
     * any feedback that was created in Artemis!
     */
    public void cancel() throws ArtemisNetworkException {
        ProgrammingSubmissionDTO.cancelAssessment(this.getConnection().getClient(), this.programmingSubmission.getId());
    }

    /**
     * Calculates the total points that the student receives for his submission.
     * This value will be shown to the student in Artemis.
     */
    public double calculateTotalPoints() {
        double points = this.calculateTotalPointsOfAnnotations();
        points += this.calculateTotalPointsOfTests();
        return Math.min(Math.max(points, 0.0), this.getMaxPoints());
    }

    /**
     * Calculates the total points of the annotations, not including points from the
     * tests.
     */
    public double calculateTotalPointsOfAnnotations() {
        return this.config.getRatingGroups().stream()
                .map(this::calculatePointsForRatingGroup)
                .mapToDouble(Points::score)
                .sum();
    }

    /**
     * Calculates the total points from (automatic) tests, as reported by Artemis.
     */
    public double calculateTotalPointsOfTests() {
        return this.testResults.stream().mapToDouble(TestResult::getPoints).sum();
    }

    /**
     * Get the maximum number of points that the student can receive for his
     * submission.
     */
    public double getMaxPoints() {
        return this.programmingSubmission.getExercise().getMaxPoints();
    }

    /**
     * Calculates the points from all annotations of a specific mistake type.
     *
     * @return Empty, if no annotations of this type are present. Otherwise, the
     * total points for the annotations.
     */
    public Optional<Points> calculatePointsForMistakeType(MistakeType mistakeType) {
        var annotationsWithType = this.getAnnotations(mistakeType).stream()
                .filter(a -> !a.isDeletedInReview())
                .toList();
        if (annotationsWithType.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mistakeType.getRule().calculatePoints(annotationsWithType));
    }

    /**
     * Calculates the total points of all annotations that are part of the given
     * rating group.
     */
    public Points calculatePointsForRatingGroup(RatingGroup ratingGroup) {
        double points = this.streamNonDeletedAnnotations()
                .filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup))
                .filter(a -> a.getMistakeType().shouldScore())
                .collect(Collectors.groupingBy(Annotation::getMistakeType))
                .entrySet()
                .stream()
                .mapToDouble(
                        e -> e.getKey().getRule().calculatePoints(e.getValue()).score())
                .sum();
        if (points > ratingGroup.getMaxPenalty()) {
            return new Points(ratingGroup.getMaxPenalty(), true);
        } else if (points < ratingGroup.getMinPenalty()) {
            return new Points(ratingGroup.getMinPenalty(), true);
        } else {
            return new Points(points, false);
        }
    }

    public GradingConfig getConfig() {
        return config;
    }

    public CorrectionRound getCorrectionRound() {
        return correctionRound;
    }

    public List<TestResult> getTestResults() {
        return new ArrayList<>(this.testResults);
    }

    private void internalSaveOrSubmit(boolean shouldSubmit) throws AnnotationMappingException, ArtemisNetworkException {
        log.info("Packing assessment for artemis");
        var feedbacks = this.packAssessmentForArtemis();
        double absoluteScore = this.calculateTotalPoints();
        double relativeScore = absoluteScore / this.getMaxPoints() * 100.0;
        ResultDTO result = ResultDTO.forAssessmentSubmission(
                this.programmingSubmission.getId(), relativeScore, feedbacks, this.lockingResult);

        // Sanity check
        double feedbackPoints = Math.clamp(
                result.feedbacks().stream().mapToDouble(FeedbackDTO::credits).sum(), 0.0, this.getMaxPoints());
        if (Math.abs(absoluteScore - feedbackPoints) > 1e-7) {
            throw new IllegalStateException("Feedback points do not match the calculated points. Calculated "
                    + absoluteScore + " but feedbacks sum up to " + feedbackPoints + " points.");
        }

        ProgrammingSubmissionDTO.saveAssessment(
                this.getConnection().getClient(),
                this.programmingSubmission.getParticipationId(),
                shouldSubmit,
                result);
    }

    private List<FeedbackDTO> packAssessmentForArtemis() throws AnnotationMappingException {
        // We need all automatic feedback
        List<FeedbackDTO> feedbacks = new ArrayList<>(
                this.testResults.stream().map(TestResult::getDto).toList());

        // The autograder creates many annotations, because it highlights every single violation.
        // For Artemis, we want to group these annotations if they are more than a certain number.
        //
        // The code is mainly intended to group annotations created by the autograder, but will work
        // for any annotation that has the classifiers set.
        List<Annotation> allAnnotations =
                AnnotationMerger.mergeAnnotations(this.annotations, DEFAULT_ANNOTATION_LIMIT, this.studentLocale);

        // For each annotation we have a manual feedback at the respective line
        // These feedbacks deduct no points. They are just for the student to see in the
        // Artemis code viewer
        // We group annotations by file and line to have at most one annotation per line
        feedbacks.addAll(allAnnotations.stream()
                .collect(
                        Collectors.groupingBy(Annotation::getFilePath, Collectors.groupingBy(Annotation::getStartLine)))
                .entrySet()
                .stream()
                .flatMap(e -> e.getValue().entrySet().stream())
                .map(this::createInlineFeedback)
                .toList());

        // We have one (or more if they are too long) global feedback per rating group
        // These feedbacks deduct points
        feedbacks.addAll(this.config.getRatingGroups().stream()
                .flatMap(r -> this.createGlobalFeedback(r).stream())
                .toList());

        // All feedbacks that are created after this point are either invisible to the
        // student or test results
        // We should add a dummy feedback so that the student will see at least one
        // feedback
        if (feedbacks.isEmpty()) {
            feedbacks.add(FeedbackDTO.newVisibleManualUnreferenced(
                    0.0, null, NO_FEEDBACK_DUMMY.format().translateTo(studentLocale)));
        }

        // Add the meta feedback(s)
        feedbacks.addAll(MetaFeedbackMapper.createMetaFeedbacks(this.annotations));

        log.info(
                "Created {} manual feedbacks for artemis",
                feedbacks.stream().filter(f -> f.type() == FeedbackType.MANUAL).count());
        log.info(
                "Created {} manual-unreferenced feedbacks for artemis",
                feedbacks.stream()
                        .filter(f -> f.type() == FeedbackType.MANUAL_UNREFERENCED)
                        .count());

        return feedbacks;
    }

    private FeedbackDTO createInlineFeedback(Map.Entry<Integer, ? extends List<Annotation>> annotations) {
        var sampleAnnotation = annotations.getValue().getFirst();

        String text =
                "File " + sampleAnnotation.getFilePathWithoutType() + " at line " + sampleAnnotation.getDisplayLine();
        String reference = "file:" + sampleAnnotation.getFilePath() + "_line:" + sampleAnnotation.getStartLine();
        String detailText = annotations.getValue().stream()
                .map(a -> {
                    if (a.getMistakeType().isCustomAnnotation()) {
                        return MANUAL_FEEDBACK_CUSTOM_PENALTY
                                .format(
                                        a.getMistakeType().getRatingGroup().getDisplayName(),
                                        a.getMistakeType().getButtonText(),
                                        a.getCustomMessage().orElseThrow(),
                                        a.getCustomScore().orElseThrow())
                                .translateTo(studentLocale);
                    } else if (a.getCustomMessage().isPresent()
                            && !a.getCustomMessage().get().isBlank()) {
                        return MANUAL_FEEDBACK_CUSTOM_EXP
                                .format(
                                        a.getMistakeType().getRatingGroup().getDisplayName(),
                                        a.getMistakeType().getButtonText(),
                                        a.getMistakeType().getMessage(),
                                        a.getCustomMessage().get())
                                .translateTo(studentLocale);
                    } else {
                        return MANUAL_FEEDBACK
                                .format(
                                        a.getMistakeType().getRatingGroup().getDisplayName(),
                                        a.getMistakeType().getButtonText(),
                                        a.getMistakeType().getMessage())
                                .translateTo(studentLocale);
                    }
                })
                .collect(Collectors.joining("\n\n"))
                .trim();
        return FeedbackDTO.newManual(0.0, text, reference, detailText);
    }

    private TranslatableString formatGlobalFeedbackAnnotations(List<Annotation> annotations, boolean hasScore) {
        LocationFormatter formatter = new LocationFormatter()
                // .enableLineMerging()
                .removeSharedPrefix(true)
                // only show the starting line number
                .setLocationToString(location -> "" + (location.start().line() + 1));

        for (var annotation : annotations) {
            formatter.addLocation(annotation.getLocation());
        }

        String filePath = annotations.getFirst().getFilePath();

        if (hasScore) {
            double customScore = annotations.stream()
                    .mapToDouble(a -> a.getCustomScore().get())
                    .sum();

            FormatString formatString = GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY_MULTIPLE;
            if (annotations.size() == 1) {
                formatString = GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY;
            }

            return formatString.format(filePath, formatter.format(), customScore);
        } else {
            FormatString formatString = GLOBAL_FEEDBACK_ANNOTATION_MULTIPLE;
            if (annotations.size() == 1) {
                formatString = GLOBAL_FEEDBACK_ANNOTATION;
            }

            return formatString.format(filePath, formatter.format());
        }
    }

    /**
     * This builds one (or more if the feedback is too long) global feedback for a
     * rating group. The feedback deducts points, and lists all annotations that are
     * part of the rating group.
     */
    private List<FeedbackDTO> createGlobalFeedback(RatingGroup ratingGroup) {
        Points points = this.calculatePointsForRatingGroup(ratingGroup);

        // Header:
        // Methodik [-1 (Range: -4 -- 0) points]
        // The header is reused for every sub-feedback
        var header = GLOBAL_FEEDBACK_HEADER.format(
                ratingGroup.getDisplayName(), points.score(), ratingGroup.getMinPenalty(), ratingGroup.getMaxPenalty());

        // group annotations by mistake type
        List<Map.Entry<MistakeType, List<Annotation>>> annotationsByType = new ArrayList<>();
        for (var mistakeType : ratingGroup.getMistakeTypes()) {
            var annotationsWithType = this.getAnnotations(mistakeType, this.annotations);
            if (!annotationsWithType.isEmpty()) {
                annotationsByType.add(Map.entry(mistakeType, annotationsWithType));
            }
        }

        // Sort the entries by their size, then by their elements location
        annotationsByType.sort(Map.Entry.comparingByValue(ComparatorUtils.shortestFirst(
                ComparatorUtils.compareByElement(Comparator.comparing(Annotation::getLocation)))));

        // First collect only the lines so that we can later split the feedback by lines
        Collection<TranslatableString> lines = new ArrayList<>();
        for (var entry : annotationsByType) {
            MistakeType mistakeType = entry.getKey();
            List<Annotation> annotationsWithType = entry.getValue();

            Optional<Points> mistakePoints = this.calculatePointsForMistakeType(mistakeType);
            if (mistakePoints.isEmpty()) {
                continue;
            }

            lines.addAll(this.makeFeedbackForMistakeType(
                    ratingGroup, mistakeType, annotationsWithType, mistakePoints.get()));
        }

        // No feedback, so return early
        if (lines.isEmpty()) {
            return List.of();
        }

        // Add a remark if we hit the limits
        if (points.capped() && ratingGroup.isScoringGroup()) {
            lines.add(GLOBAL_FEEDBACK_LIMIT_OVERRUN.format());
        }

        // Possibly split into multiple feedbacks
        List<String> feedbackTexts = FeedbackSplitter.splitLines(lines, header, studentLocale);
        if (feedbackTexts.size() == 1) {
            return List.of(FeedbackDTO.newVisibleManualUnreferenced(points.score(), null, feedbackTexts.get(0)));
        } else {
            // We have more than one feedback to create
            // To make it easier for students, each feedback gets a running index
            // (annotation 1/2, annotation 2/2)
            // appended to its header
            List<FeedbackDTO> feedbacks = new ArrayList<>();
            for (int i = 0; i < feedbackTexts.size(); i++) {
                // Only the first feedback deducts points
                double score = points.score();
                if (i != 0) {
                    score = GLOBAL_FEEDBACK_OTHER_PARTS_SCORE;
                }
                feedbacks.add(FeedbackDTO.newVisibleManualUnreferenced(score, null, feedbackTexts.get(i)));
            }
            return feedbacks;
        }
    }

    private List<TranslatableString> makeFeedbackForMistakeType(
            RatingGroup ratingGroup,
            MistakeType mistakeType,
            Collection<Annotation> annotationsWithType,
            Points mistakePoints) {
        List<TranslatableString> lines = new ArrayList<>();

        // Header per mistake type
        if (ratingGroup.isScoringGroup() && mistakeType.shouldScore()) {
            lines.add(GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER.format(mistakeType.getButtonText(), mistakePoints.score()));
        } else {
            // We don't want to display points if the rating group does not score
            lines.add(GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER_NONSCORING.format(mistakeType.getButtonText()));
        }

        var annotationsByFilePath = annotationsWithType.stream()
                .filter(a -> !a.isDeletedInReview())
                .collect(Collectors.groupingBy(Annotation::getFilePath, LinkedHashMap::new, Collectors.toList()));

        for (var annotationsForPath : annotationsByFilePath.values()) {
            // Individual annotations
            Predicate<Annotation> hasScore = a -> a.getCustomScore().isPresent() && ratingGroup.isScoringGroup();

            // separate annotations with and without score
            List<Annotation> annotationsWithScore =
                    annotationsForPath.stream().filter(hasScore).toList();
            List<Annotation> annotationsWithoutScore =
                    annotationsForPath.stream().filter(Predicate.not(hasScore)).toList();

            // For custom annotations, we have '* <file> at line <line> (<score>P)'
            if (!annotationsWithScore.isEmpty()) {
                lines.add(this.formatGlobalFeedbackAnnotations(annotationsWithScore, true));
            }

            // Otherwise, it's just '* <file> at line <line>'
            // Lines are zero-indexed
            if (!annotationsWithoutScore.isEmpty()) {
                lines.add(this.formatGlobalFeedbackAnnotations(annotationsWithoutScore, false));
            }
        }

        return lines;
    }
}
