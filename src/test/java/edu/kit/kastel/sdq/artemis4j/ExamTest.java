/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.Exam;
import edu.kit.kastel.sdq.artemis4j.grading.ExamExerciseGroup;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExamTest {
	private static final String ROUND_ONE_FEEDBACK = "feedback round 1";
	private static final String ROUND_TWO_FEEDBACK = "feedback round 2";

	private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
	private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
	private static final String STUDENT_USER = System.getenv("STUDENT_USER");
	private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
	private static final String COURSE_ID = System.getenv("COURSE_ID");
	private static final String EXAM_ID = System.getenv("EXAM_ID");
	private static final String EXERCISE_GROUP_ID = System.getenv("EXERCISE_GROUP_ID");
	private static final String PROGRAMMING_EXERCISE_ID = System.getenv("PROGRAMMING_EXERCISE_ID");

	@Test
	void testExamAssessment() throws ArtemisClientException, IOException {
		ArtemisInstance artemis = new ArtemisInstance(ARTEMIS_URL);
		ArtemisConnection connection = ArtemisConnection.connectWithUsernamePassword(artemis, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);
		Course course = connection.getCourseById(Integer.parseInt(COURSE_ID));
		Exam exam = course.getExamById(Integer.parseInt(EXAM_ID));
		ExamExerciseGroup exerciseGroup = exam.getExerciseGroupById(Integer.parseInt(EXERCISE_GROUP_ID));
		ProgrammingExercise exercise = exerciseGroup.getProgrammingExerciseById(Integer.parseInt(PROGRAMMING_EXERCISE_ID));

		GradingConfig config = GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/config.json")), exercise);

		ProgrammingSubmission roundOneSubmission = findSubmission(exercise.fetchSubmissions(0), STUDENT_USER);
		Assessment roundOneAssessment = roundOneSubmission.tryLock(config).orElseThrow();
		roundOneAssessment.clearAnnotations();
		roundOneAssessment.addCustomAnnotation(config.getMistakeTypeById("custom"), "src/edu/kit/informatik/BubbleSort.java", 1, 2, ROUND_ONE_FEEDBACK, -2.0);
		roundOneAssessment.submit(Locale.GERMANY);

		ProgrammingSubmission roundTwoSubmission = findSubmission(exercise.fetchSubmissions(1), STUDENT_USER);
		Assessment roundTwoAssessment = roundTwoSubmission.tryLock(config).orElseThrow();
		roundTwoAssessment.addCustomAnnotation(config.getMistakeTypeById("custom"), "src/edu/kit/informatik/BubbleSort.java", 2, 3, ROUND_TWO_FEEDBACK, -1.0);

		var annotations = roundTwoAssessment.getAnnotations();
		assertEquals(2, annotations.size());
		assertTrue(annotations.stream()
				.anyMatch(a -> a.getSource() == AnnotationSource.MANUAL_FIRST_ROUND && a.getCustomMessage().equals(Optional.of(ROUND_ONE_FEEDBACK))));
		assertTrue(annotations.stream()
				.anyMatch(a -> a.getSource() == AnnotationSource.MANUAL_SECOND_ROUND && a.getCustomMessage().equals(Optional.of(ROUND_TWO_FEEDBACK))));

		roundTwoAssessment.submit(Locale.GERMANY);
	}

	private ProgrammingSubmission findSubmission(List<ProgrammingSubmission> submissions, String student) {
		return submissions.stream().filter(submission -> submission.getParticipantIdentifier().equals(student)).findFirst().orElseThrow();
	}
}
