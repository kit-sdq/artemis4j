/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.*;
import edu.kit.kastel.sdq.artemis4j.client.RestClientManager;
import edu.kit.kastel.sdq.artemis4j.grading.artemis.AnnotationMapper;
import edu.kit.kastel.sdq.artemis4j.grading.config.ExerciseConfig;
import edu.kit.kastel.sdq.artemis4j.grading.config.JsonFileConfig;
import edu.kit.kastel.sdq.artemis4j.grading.model.annotation.Annotation;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class End2EndTest {

	private static final String FEEDBACK_TEXT = "This is a test feedback. To make it very long, it will just repeated over and over again. ".repeat(30).trim();

	// Configure a simple Programming exercise (Sorting algorithms in Artemis
	// (Default Template: Package Name: edu.kit.informatik))
	private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
	private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
	private static final String STUDENT_USER = System.getenv("STUDENT_USER");
	private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
	private static final String COURSE_ID = System.getenv("COURSE_ID");
	private static final String PROGRAMMING_EXERCISE_ID = System.getenv("PROGRAMMING_EXERCISE_ID");

	private final File configuration = new File("src/test/resources/config.json");

	private RestClientManager restClientManager;
	private Course course;
	private Exercise exercise;
	private Submission submission;
	private LockResult lock;
	private ExerciseConfig config;

	@BeforeAll
	public void checkConfiguration() {
		Assertions.assertNotNull(INSTRUCTOR_USER);
		Assertions.assertNotNull(INSTRUCTOR_PASSWORD);
		Assertions.assertNotNull(STUDENT_USER);
		Assertions.assertNotNull(ARTEMIS_URL);
		Assertions.assertNotNull(COURSE_ID);
		Assertions.assertNotNull(PROGRAMMING_EXERCISE_ID);
	}

	@BeforeEach
	public void setup() throws ArtemisClientException, IOException {
		restClientManager = new RestClientManager(ARTEMIS_URL, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);
		restClientManager.login();
		this.course = restClientManager.getCourseArtemisClient().getCourses().stream().filter(c -> c.getCourseId() == Integer.parseInt(COURSE_ID)).findFirst()
				.orElseThrow();
		this.exercise = course.getExercises().stream().filter(e -> e.getExerciseId() == Integer.parseInt(PROGRAMMING_EXERCISE_ID)).findFirst().orElseThrow();
		var submissions = restClientManager.getSubmissionArtemisClient().getSubmissions(this.exercise);
		this.submission = submissions.stream().filter(a -> a.getParticipantIdentifier().equals(STUDENT_USER)).findFirst().orElseThrow();
		this.lock = this.restClientManager.getAssessmentArtemisClient().startAssessment(this.submission);
		this.cleanupFeedback();
		this.lock = this.restClientManager.getAssessmentArtemisClient().startAssessment(this.submission);

		for (var feedback : this.lock.getLatestFeedback()) {
			Assertions.assertEquals(FeedbackType.AUTOMATIC, feedback.getFeedbackType());
		}

		JsonFileConfig jsonFileConfig = new JsonFileConfig(configuration);
		config = jsonFileConfig.getExerciseConfig(this.exercise);
	}

	private void cleanupFeedback() throws ArtemisClientException {
		var feedbackAutomatic = this.lock.getLatestFeedback().stream().filter(f -> f.getFeedbackType() == FeedbackType.AUTOMATIC).toList();
		final List<Feedback> tests = this.lock.getLatestFeedback().stream().filter(f -> f.getCodeLocation() == null).toList();

		int codeIssueCount = (int) this.lock.getLatestFeedback().stream().filter(Feedback::isStaticCodeAnalysis).count();
		int passedTestCaseCount = (int) tests.stream().filter(feedback -> feedback.getPositive() != null && feedback.getPositive()).count();
		double absoluteScore = tests.stream().mapToDouble(Feedback::getCredits).sum();

		AssessmentResult assessmentResult = new AssessmentResult(this.lock.getSubmissionId(), "SEMI_AUTOMATIC",
				absoluteScore / this.exercise.getMaxPoints() * 100D, true, this.restClientManager.getAuthenticationClient().getUser(), feedbackAutomatic,
				codeIssueCount, passedTestCaseCount, tests.size());
		this.restClientManager.getAssessmentArtemisClient().saveAssessment(this.lock.getParticipationId(), true, assessmentResult);
	}

	@Test
	void testCreationOfSimpleAnnotations() throws IOException, ArtemisClientException {
		Annotation annotation = new Annotation(UUID.randomUUID().toString(), config.getMistakeTypes().get(1), 1, 2, "src/edu/kit/informatik/BubbleSort", null,
				null);
		AnnotationMapper annotationMapper = new AnnotationMapper(exercise, submission, List.of(annotation), config.getIRatingGroups(),
				this.restClientManager.getAuthenticationClient().getUser(), lock);
		var result = annotationMapper.createAssessmentResult();
		this.restClientManager.getAssessmentArtemisClient().saveAssessment(this.lock.getParticipationId(), true, result);

		// Check Assessments
		this.lock = this.restClientManager.getAssessmentArtemisClient().startAssessment(this.submission);
		List<Feedback> allFeedback = this.lock.getLatestFeedback();
		List<Feedback> tests = allFeedback.stream().filter(f -> f.getFeedbackType() == FeedbackType.AUTOMATIC && f.getCodeLocation() == null).toList();
		Assertions.assertEquals(13, tests.size());

		List<Feedback> clientData = allFeedback.stream()
				.filter(f -> f.getFeedbackType() == FeedbackType.MANUAL_UNREFERENCED && Objects.equals(f.getCodeLocationHumanReadable(), "CLIENT_DATA"))
				.toList();
		Assertions.assertEquals(1, clientData.size());

		List<Feedback> manualFeedback = allFeedback.stream().filter(f -> f.getFeedbackType() == FeedbackType.MANUAL).toList();
		Assertions.assertEquals(1, manualFeedback.size());
	}

	@Test
	void testCreationOfCustomAnnotations() throws IOException, ArtemisClientException {
		Annotation annotation = new Annotation(UUID.randomUUID().toString(), config.getMistakeTypes().get(0), 1, 2, "src/edu/kit/informatik/BubbleSort",
				FEEDBACK_TEXT, -2.0);
		AnnotationMapper annotationMapper = new AnnotationMapper(exercise, submission, List.of(annotation), config.getIRatingGroups(),
				this.restClientManager.getAuthenticationClient().getUser(), lock);
		var result = annotationMapper.createAssessmentResult();
		this.restClientManager.getAssessmentArtemisClient().saveAssessment(this.lock.getParticipationId(), true, result);

		// Check Assessments
		this.lock = this.restClientManager.getAssessmentArtemisClient().startAssessment(this.submission);
		List<Feedback> allFeedback = this.lock.getLatestFeedback();
		List<Feedback> tests = allFeedback.stream().filter(f -> f.getFeedbackType() == FeedbackType.AUTOMATIC && f.getCodeLocation() == null).toList();
		Assertions.assertEquals(13, tests.size());

		List<Feedback> clientData = allFeedback.stream()
				.filter(f -> f.getFeedbackType() == FeedbackType.MANUAL_UNREFERENCED && Objects.equals(f.getCodeLocationHumanReadable(), "CLIENT_DATA"))
				.toList();
		Assertions.assertEquals(1, clientData.size());
		Assertions.assertTrue(clientData.get(0).getDetailText().contains(FEEDBACK_TEXT));

		List<Feedback> manualFeedback = allFeedback.stream().filter(f -> f.getFeedbackType() == FeedbackType.MANUAL).toList();
		Assertions.assertEquals(1, manualFeedback.size());
		Assertions.assertTrue(manualFeedback.get(0).getDetailText().contains(FEEDBACK_TEXT));
	}

}
