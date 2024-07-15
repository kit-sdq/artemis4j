/* Licensed under EPL-2.0 2023-2024. */
package edu.kit.kastel.sdq.artemis4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

		this.gradingConfig = GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/config.json")), this.exercise);

		// ensure that the submission is locked
		this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();
		this.cleanupFeedback();
		this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

		Assertions.assertTrue(this.assessment.getAnnotations().isEmpty());
	}

	private void cleanupFeedback() throws ArtemisClientException {
		this.assessment.clearAnnotations();
		this.assessment.submit();
	}

	@Test
	void testCreationOfSimpleAnnotations() throws ArtemisClientException {
		MistakeType mistakeType = this.gradingConfig.getMistakeTypes().get(1);

		this.assessment.addPredefinedAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", // TODO: the file path did not have .java before
				1, 2, null);

		this.assessment.submit();

		// Check Assessments
		this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

		List<TestResult> tests = this.assessment.getTestResults();
		Assertions.assertEquals(13, tests.size());

		Assertions.assertEquals(1, this.assessment.getAnnotations().size());
		Assertions.assertEquals(AnnotationSource.MANUAL_FIRST_ROUND, this.assessment.getAnnotations().get(0).getSource());
	}

	@Test
	void testCreationOfCustomAnnotation() throws ArtemisClientException {
		MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("custom");

		this.assessment.addCustomAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", 1, 2, FEEDBACK_TEXT, -2.0);
		this.assessment.submit();

		// Check Assessments
		this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

		List<TestResult> tests = this.assessment.getTestResults();
		Assertions.assertEquals(13, tests.size());

		Assertions.assertEquals(1, this.assessment.getAnnotations().size());
		var annotation = this.assessment.getAnnotations().get(0);
		Assertions.assertEquals(AnnotationSource.MANUAL_FIRST_ROUND, annotation.getSource());
		Assertions.assertEquals(Optional.of(-2.0), annotation.getCustomScore());
		Assertions.assertEquals(Optional.of(FEEDBACK_TEXT), annotation.getCustomMessage());
	}
}
