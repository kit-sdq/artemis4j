/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
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
 * stores annotations, calculates points, and parses & serializes feedbacks for
 * Artemis.
 */
public class Assessment extends ArtemisConnectionHolder {
	private static final Logger log = org.slf4j.LoggerFactory.getLogger(Assessment.class);

	private static final FormatString MANUAL_FEEDBACK = new FormatString(new MessageFormat("[{0}:{1}] {2}"));
	private static final FormatString MANUAL_FEEDBACK_CUSTOM_EXP = new FormatString(new MessageFormat("[{0}:{1}] " + "{2}\nExplanation: {3}"));
	private static final FormatString MANUAL_FEEDBACK_CUSTOM_PENALTY = new FormatString(new MessageFormat("[{0}:{1}] " + "{2} ({3,number,##.###}P)"));
	private static final FormatString GLOBAL_FEEDBACK_HEADER = new FormatString(
			new MessageFormat("{0} [{1,number,##" + ".###}P (Range: {2,number,##.###}P -- {3,number,##.###}P)]"));
	private static final FormatString GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER = new FormatString(new MessageFormat("    *" + " {0} [{1,number,##.###}P]:"));
	private static final FormatString GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER_NONSCORING = new FormatString(new MessageFormat("    * {0}:"));
	private static final FormatString GLOBAL_FEEDBACK_ANNOTATION = new FormatString(new MessageFormat("        * {0} " + "at line {1,number}"));
	private static final FormatString GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY = new FormatString(
			new MessageFormat("        * {0} at line {1,number} ({2,number,##.###}P)"));
	private static final FormatString GLOBAL_FEEDBACK_LIMIT_OVERRUN = new FormatString(
			new MessageFormat("    * Note:" + " The sum of penalties hit the limits for this rating group."));
	private static final FormatString NO_FEEDBACK_DUMMY = new FormatString("The tutor has made no annotations.");

	private final List<Annotation> annotations;
	private final List<TestResult> testResults;
	private final ProgrammingSubmission programmingSubmission;
	private final GradingConfig config;
	private final int correctionRound;
	private final Locale studentLocale;

	public Assessment(ResultDTO result, GradingConfig config, ProgrammingSubmission programmingSubmission, int correctionRound)
			throws AnnotationMappingException {
		this(result, config, programmingSubmission, correctionRound, Locale.GERMANY);
	}

	public Assessment(ResultDTO result, GradingConfig config, ProgrammingSubmission programmingSubmission, int correctionRound, Locale studentLocale)
			throws AnnotationMappingException {
		super(programmingSubmission);
		this.programmingSubmission = programmingSubmission;
		this.config = config;
		this.correctionRound = correctionRound;
		this.studentLocale = studentLocale;

		// Unpack the result
		this.annotations = MetaFeedbackMapper.parseMetaFeedbacks(result.feedbacks(), config);
		this.testResults = result.feedbacks().stream().filter(f -> f.type() == FeedbackType.AUTOMATIC).map(TestResult::new).toList();
	}

	/**
	 * Get the submission associated with this assessment
	 *
	 * @return
	 */
	public ProgrammingSubmission getSubmission() {
		return programmingSubmission;
	}

	/**
	 * Get the annotations that are present. Use the add/remove methods to modify
	 * the list of annotations.
	 *
	 * @return An unmodifiable list of annotations.
	 */
	public List<Annotation> getAnnotations() {
		return Collections.unmodifiableList(this.annotations);
	}

	/**
	 * Gets all annotations associated with the specified mistake type. The mistake
	 * type must be associated with the same grading config as this assessment.
	 *
	 * @param mistakeType
	 * @return An unmodifiable list of annotations, possibly empty but never null.
	 */
	public List<Annotation> getAnnotations(MistakeType mistakeType) {
		return this.annotations.stream().filter(a -> a.getMistakeType().equals(mistakeType)).toList();
	}

	/**
	 * Gets all annotations associated with the specified rating group. The rating
	 * group must be associated with the same grading config as this assessment.
	 *
	 * @param ratingGroup
	 * @return An unmodifiable list of annotations, possibly empty but never null.
	 */
	public List<Annotation> getAnnotations(RatingGroup ratingGroup) {
		return this.annotations.stream().filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup)).toList();
	}

	/**
	 * Adds a non-custom manual annotation to the assessment.
	 *
	 * @param mistakeType   Must not be a custom mistake type
	 * @param filePath
	 * @param startLine
	 * @param endLine
	 * @param customMessage May be null if no custom message is provided
	 */
	public void addPredefinedAnnotation(MistakeType mistakeType, String filePath, int startLine, int endLine, String customMessage) {
		if (mistakeType.isCustomAnnotation()) {
			throw new IllegalArgumentException("Mistake type is a custom annotation");
		}

		var source = this.correctionRound == 0 ? AnnotationSource.MANUAL_FIRST_ROUND : AnnotationSource.MANUAL_SECOND_ROUND;
		this.annotations.add(new Annotation(mistakeType, filePath, startLine, endLine, customMessage, null, source));
	}

	/**
	 * Adds a custom manual annotation to the assessment.
	 *
	 * @param mistakeType   Must be a custom mistake type
	 * @param filePath
	 * @param startLine
	 * @param endLine
	 * @param customMessage May not be null
	 * @param customScore
	 */
	public void addCustomAnnotation(MistakeType mistakeType, String filePath, int startLine, int endLine, String customMessage, double customScore) {
		if (!mistakeType.isCustomAnnotation()) {
			throw new IllegalArgumentException("Mistake type is not a custom annotation");
		}

		if (customScore > 0.0 && !this.config.isPositiveFeedbackAllowed()) {
			throw new IllegalArgumentException("Custom annotations with positive scores are not allowed for this exercise");
		}

		var source = this.correctionRound == 0 ? AnnotationSource.MANUAL_FIRST_ROUND : AnnotationSource.MANUAL_SECOND_ROUND;
		this.annotations.add(new Annotation(mistakeType, filePath, startLine, endLine, customMessage, customScore, source));
	}

	public void addAutograderAnnotation(MistakeType mistakeType, String filePath, int startLine, int endLine, String explanation) {
		Double customScore = mistakeType.isCustomAnnotation() ? 0.0 : null;
		this.annotations.add(new Annotation(mistakeType, filePath, startLine, endLine, explanation, customScore, AnnotationSource.AUTOGRADER));
	}

	/**
	 * Removes an annotation from the assessment. If the annotation is not present,
	 * nothing happens.
	 *
	 * @param annotation
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
	 *
	 * @throws AnnotationMappingException
	 * @throws ArtemisNetworkException
	 */
	public void save() throws AnnotationMappingException, ArtemisNetworkException {
		this.internalSaveOrSubmit(false);
	}

	/**
	 * Saves and submits the assessment. A submitted assessment can still be changed
	 * if you have its ID, but it will be marked as assessed. This also frees the
	 * lock on the submission.
	 *
	 * @throws AnnotationMappingException
	 * @throws ArtemisNetworkException
	 */
	public void submit() throws AnnotationMappingException, ArtemisNetworkException {
		this.internalSaveOrSubmit(true);
	}

	/**
	 * Cancels the assessment & frees the lock on the submission. This deletes any
	 * feedback that was created in Artemis!
	 *
	 * @throws ArtemisNetworkException
	 */
	public void cancel() throws ArtemisNetworkException {
		ProgrammingSubmissionDTO.cancelAssessment(this.getConnection().getClient(), this.programmingSubmission.getId());
	}

	/**
	 * Calculates the total points that the student receives for his submission.
	 * This value will be shown to the student in Artemis.
	 *
	 * @return
	 */
	public double calculateTotalPoints() {
		double points = this.calculateTotalPointsOfAnnotations();
		points += this.calculateTotalPointsOfTests();
		return Math.min(Math.max(points, 0.0), this.getMaxPoints());
	}

	/**
	 * Calculates the total points of the annotations, not including points from the
	 * tests.
	 *
	 * @return
	 */
	public double calculateTotalPointsOfAnnotations() {
		return this.config.getRatingGroups().stream().map(this::calculatePointsForRatingGroup).mapToDouble(Points::score).sum();
	}

	/**
	 * Calculates the total points from (automatic) tests, as reported by Artemis.
	 *
	 * @return
	 */
	public double calculateTotalPointsOfTests() {
		return this.testResults.stream().mapToDouble(TestResult::getPoints).sum();
	}

	/**
	 * Get the maximum number of points that the student can receive for his
	 * submission.
	 *
	 * @return
	 */
	public double getMaxPoints() {
		return this.programmingSubmission.getExercise().getMaxPoints();
	}

	/**
	 * Calculates the points from all annotations of a specific mistake type.
	 *
	 * @param mistakeType
	 * @return Empty, if no annotations of this type are present. Otherwise, the
	 *         total points for the annotations.
	 */
	public Optional<Points> calculatePointsForMistakeType(MistakeType mistakeType) {
		var annotationsWithType = this.getAnnotations(mistakeType);
		if (annotationsWithType.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(mistakeType.getRule().calculatePoints(annotationsWithType));
	}

	/**
	 * Calculates the total points of all annotations that are part of the given
	 * rating group.
	 *
	 * @param ratingGroup
	 * @return
	 */
	public Points calculatePointsForRatingGroup(RatingGroup ratingGroup) {
		double points = this.annotations.stream().filter(a -> a.getMistakeType().getRatingGroup().equals(ratingGroup))
				.filter(a -> a.getMistakeType().shouldScore()).collect(Collectors.groupingBy(Annotation::getMistakeType)).entrySet().stream()
				.mapToDouble(e -> e.getKey().getRule().calculatePoints(e.getValue()).score()).sum();
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

	public int getCorrectionRound() {
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
		ResultDTO result = ResultDTO.forAssessmentSubmission(this.programmingSubmission.getId(), relativeScore, feedbacks,
				this.getConnection().getAssessor().toDTO());

		// Sanity check
		double feedbackPoints = Math.min(Math.max(result.feedbacks().stream().mapToDouble(FeedbackDTO::credits).sum(), 0.0), this.getMaxPoints());
		if (Math.abs(absoluteScore - feedbackPoints) > 1e-7) {
			throw new IllegalStateException("Feedback points do not match the calculated points. Calculated " + absoluteScore + " but feedbacks sum up to "
					+ feedbackPoints + " points.");
		}

		ProgrammingSubmissionDTO.saveAssessment(this.getConnection().getClient(), this.programmingSubmission.getParticipationId(), shouldSubmit, result);
	}

	private List<FeedbackDTO> packAssessmentForArtemis() throws AnnotationMappingException {
		// We need all automatic feedback
		List<FeedbackDTO> feedbacks = new ArrayList<>(this.testResults.stream().map(TestResult::getDto).toList());

		// For each annotation we have a manual feedback at the respective line
		// These feedbacks deduct no points. They are just for the student to see in the
		// Artemis code viewer
		// We group annotations by file and line to have at most one annotation per line
		feedbacks.addAll(this.annotations.stream().collect(Collectors.groupingBy(Annotation::getFilePath, Collectors.groupingBy(Annotation::getStartLine)))
				.entrySet().stream().flatMap(e -> e.getValue().entrySet().stream()).map(this::createInlineFeedback).toList());

		// We have on (or more if they are too long) global feedback per rating group
		// These feedbacks deduct points
		feedbacks.addAll(this.config.getRatingGroups().stream().flatMap(r -> this.createGlobalFeedback(r).stream()).toList());

		// All feedbacks that are created after this point are either invisible to the
		// student or test results
		// We should add a dummy feedback so that the student will see at least one
		// feedback
		if (feedbacks.isEmpty()) {
			feedbacks.add(FeedbackDTO.newVisibleManualUnreferenced(0.0, null, NO_FEEDBACK_DUMMY.format().translateTo(studentLocale)));
		}

		// Add the meta feedback(s)
		feedbacks.addAll(MetaFeedbackMapper.createMetaFeedbacks(this.annotations));

		log.info("Created {} manual feedbacks for artemis", feedbacks.stream().filter(f -> f.type() == FeedbackType.MANUAL).count());
		log.info("Created {} manual-unreferenced feedbacks for artemis", feedbacks.stream().filter(f -> f.type() == FeedbackType.MANUAL_UNREFERENCED).count());

		return feedbacks;
	}

	private FeedbackDTO createInlineFeedback(Map.Entry<Integer, List<Annotation>> annotations) {
		var sampleAnnotation = annotations.getValue().get(0);

		String text = "File " + sampleAnnotation.getFilePathWithoutType() + " at line " + sampleAnnotation.getDisplayLine();
		String reference = "file:" + sampleAnnotation.getFilePath() + "_line:" + sampleAnnotation.getStartLine();
		String detailText = annotations.getValue().stream().map(a -> {
			if (a.getMistakeType().isCustomAnnotation()) {
				return MANUAL_FEEDBACK_CUSTOM_PENALTY.format(a.getMistakeType().getRatingGroup().getDisplayName(), a.getMistakeType().getButtonText(),
						a.getCustomMessage().orElseThrow(), a.getCustomScore().orElseThrow()).translateTo(studentLocale);
			} else if (a.getCustomMessage().isPresent() && !a.getCustomMessage().get().isBlank()) {
				return MANUAL_FEEDBACK_CUSTOM_EXP.format(a.getMistakeType().getRatingGroup().getDisplayName(), a.getMistakeType().getButtonText(),
						a.getMistakeType().getMessage(), a.getCustomMessage().get()).translateTo(studentLocale);
			} else {
				return MANUAL_FEEDBACK
						.format(a.getMistakeType().getRatingGroup().getDisplayName(), a.getMistakeType().getButtonText(), a.getMistakeType().getMessage())
						.translateTo(studentLocale);
			}
		}).collect(Collectors.joining("\n\n")).trim();
		return FeedbackDTO.newManual(0.0, text, reference, detailText);
	}

	/**
	 * This builds one (or more if the feedback is too long) global feedback for a
	 * rating group. The feedback deducts points, and lists all annotations that are
	 * part of the rating group.
	 *
	 * @param ratingGroup
	 * @return
	 */
	private List<FeedbackDTO> createGlobalFeedback(RatingGroup ratingGroup) {
		Points points = this.calculatePointsForRatingGroup(ratingGroup);

		// Header:
		// Methodik [-1 (Range: -4 -- 0) points]
		// The header is reused for every sub-feedback
		var header = GLOBAL_FEEDBACK_HEADER.format(ratingGroup.getDisplayName(), points.score(), ratingGroup.getMinPenalty(), ratingGroup.getMaxPenalty());

		// First collect only the lines so that we can later split the feedback by lines
		List<TranslatableString> lines = new ArrayList<>();
		for (var mistakeType : ratingGroup.getMistakeTypes()) {
			Optional<Points> mistakePoints = this.calculatePointsForMistakeType(mistakeType);
			if (mistakePoints.isPresent()) {

				// Header per mistake type
				if (ratingGroup.isScoringGroup()) {
					lines.add(GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER.format(mistakeType.getButtonText(), mistakePoints.get().score()));
				} else {
					// We don't want to display points if the rating group does not score
					lines.add(GLOBAL_FEEDBACK_MISTAKE_TYPE_HEADER_NONSCORING.format(mistakeType.getButtonText()));
				}

				// Individual annotations
				for (var annotation : this.getAnnotations(mistakeType)) {
					// For custom annotations, we have '* <file> at line <line> (<score>P)'
					// Otherwise, it's just '* <file> at line <line>'
					// Lines are zero-indexed
					if (annotation.getCustomScore().isPresent() && ratingGroup.isScoringGroup()) {
						lines.add(GLOBAL_FEEDBACK_ANNOTATION_CUSTOM_PENALTY.format(annotation.getFilePath(), annotation.getDisplayLine(),
								annotation.getCustomScore().get()));
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
				feedbacks.add(FeedbackDTO.newVisibleManualUnreferenced(i == 0 ? points.score() : 0.0, null, feedbackTexts.get(i)));
			}
			return feedbacks;
		}
	}
}
