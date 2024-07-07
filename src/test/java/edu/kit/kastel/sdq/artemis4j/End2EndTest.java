/* Licensed under EPL-2.0 2023-2024. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;

import edu.kit.kastel.sdq.artemis4j.grading.TestResult;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

	// TODO: does it make sense to have the ArtemisInstance class? Why not make it a
	// part of connection?

	private ArtemisInstance artemisInstance;
	private ArtemisConnection connection;
	private Course course;
	private ProgrammingExercise exercise;
	private ProgrammingSubmission programmingSubmission;
	private Assessment assessment;
	private GradingConfig gradingConfig;

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
		this.artemisInstance = new ArtemisInstance(ARTEMIS_URL);
		this.connection = ArtemisConnection.connectWithUsernamePassword(this.artemisInstance, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);

		this.course = this.connection.getCourses().stream().filter(c -> c.getId() == Integer.parseInt(COURSE_ID)).findFirst().orElseThrow();
		this.exercise = this.course.getProgrammingExercises().stream().filter(e -> e.getId() == Long.parseLong(PROGRAMMING_EXERCISE_ID)).findFirst()
				.orElseThrow();

		var submissions = this.exercise.fetchSubmissions();
		this.programmingSubmission = submissions.stream().filter(a -> a.getParticipantIdentifier().equals(STUDENT_USER)).findFirst().orElseThrow();

		this.gradingConfig = GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/e2e-config.json")), this.exercise);

		// ensure that the submission is locked
		this.assessment = this.exercise.tryLockSubmission(this.programmingSubmission, this.gradingConfig).orElseThrow();
		this.cleanupFeedback();
		this.assessment = this.exercise.tryLockSubmission(this.programmingSubmission, this.gradingConfig).orElseThrow();

		Assertions.assertTrue(this.assessment.getAnnotations().isEmpty());
	}

	private void cleanupFeedback() throws ArtemisClientException {
		this.assessment.clearAnnotations();
		this.assessment.submit(Locale.GERMANY);
	}

	@Test
	void testCreationOfSimpleAnnotations() throws ArtemisClientException {
		MistakeType mistakeType = this.gradingConfig.getMistakeTypes().get(1);

		this.assessment.addPredefinedAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", // TODO: the file path did not have .java before
				1, 2, null);

		this.assessment.submit(Locale.GERMANY);

		// Check Assessments
		this.assessment = this.exercise.tryLockSubmission(this.programmingSubmission, this.gradingConfig).orElseThrow();

		List<TestResult> tests = this.assessment.getTestResults();
		Assertions.assertEquals(13, tests.size());

		Assertions.assertEquals(1, this.assessment.getAnnotations().size());
		Assertions.assertEquals(AnnotationSource.MANUAL_FIRST_ROUND, this.assessment.getAnnotations().getFirst().getSource());
	}

	/*
	 * 
	 * @Test void testCreationOfCustomAnnotations() throws IOException,
	 * ArtemisClientException { Annotation annotation = new
	 * Annotation(UUID.randomUUID().toString(), config.getMistakeTypes().get(0), 1,
	 * 2, "src/edu/kit/informatik/BubbleSort", FEEDBACK_TEXT, -2.0);
	 * AnnotationMapper annotationMapper = new AnnotationMapper(exercise,
	 * submission, List.of(annotation), config.getIRatingGroups(),
	 * this.restClientManager.getAuthenticationClient().getUser(), lock); var result
	 * = annotationMapper.createAssessmentResult();
	 * this.restClientManager.getAssessmentArtemisClient().saveAssessment(this.lock.
	 * getParticipationId(), true, result);
	 * 
	 * // Check Assessments this.lock =
	 * this.restClientManager.getAssessmentArtemisClient().startAssessment(this.
	 * submission); List<Feedback> allFeedback = this.lock.getLatestFeedback();
	 * List<Feedback> tests = allFeedback.stream().filter(f -> f.getFeedbackType()
	 * == FeedbackType.AUTOMATIC && f.getCodeLocation() == null).toList();
	 * Assertions.assertEquals(13, tests.size());
	 * 
	 * List<Feedback> clientData = allFeedback.stream() .filter(f ->
	 * f.getFeedbackType() == FeedbackType.MANUAL_UNREFERENCED &&
	 * Objects.equals(f.getCodeLocationHumanReadable(), "CLIENT_DATA")) .toList();
	 * Assertions.assertEquals(1, clientData.size());
	 * Assertions.assertTrue(clientData.get(0).getDetailText().contains(
	 * FEEDBACK_TEXT));
	 * 
	 * List<Feedback> manualFeedback = allFeedback.stream().filter(f ->
	 * f.getFeedbackType() == FeedbackType.MANUAL).toList();
	 * Assertions.assertEquals(1, manualFeedback.size());
	 * Assertions.assertTrue(manualFeedback.get(0).getDetailText().contains(
	 * FEEDBACK_TEXT)); }
	 */

}
