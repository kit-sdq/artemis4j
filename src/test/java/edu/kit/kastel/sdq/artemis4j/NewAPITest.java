/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.autograder.AutograderFailedException;
import edu.kit.kastel.sdq.artemis4j.grading.autograder.AutograderRunner;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NewAPITest {
	private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
	private static final String ARTEMIS_USERNAME = System.getenv("ARTEMIS_USER");
	private static final String ARTEMIS_PASSWORD = System.getenv("ARTEMIS_PASSWORD");

	@Test
	void testLogin() throws ArtemisClientException, IOException {
		// An ArtemisInstance describes where we want to connect to
		// For the user it's just a fancy wrapper around a URL
		var artemis = new ArtemisInstance(ARTEMIS_URL);

		// Let's connect to Artemis
		// This performs username & password authentication, but you can also supply a
		// token
		var connection = ArtemisConnection.connectWithUsernamePassword(artemis, ARTEMIS_USERNAME, ARTEMIS_PASSWORD);
		System.out.println("User is " + connection.getAssessor().getLogin());

		// Fetch all courses, and get the first one (not the course with id 0!)
		// Network requests are generally only performed once when required, and the
		// results are cached
		var course = connection.getCourses().getFirst();
		System.out.println("Course is " + course.getTitle());

		// Get the first exercise (not the exercise with id 0!) in the course
		var exercise = course.getProgrammingExercises().getFirst();
		System.out.println("Exercise is " + exercise.getTitle());

		// For grading, we need the grading config
		// A config is always tailored to a specific exercise, and the constructor
		// method throws if
		// they don't match
		var gradingConfig = GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/grading-config-sheet1taskB.json")), exercise);

		// We lock the submission with id 524 for the first correction round
		// You can also use tryLockNextSubmission(correctionRound, gradingConfig) to
		// request the next submission to grade
		// without supplying an id
		var assessment = exercise.tryLockSubmission(531, 0, gradingConfig).orElseThrow();
		assessment.clearAnnotations();

		// Let's clone the test repository & submission into a temporary directory
		// The test repo will be cloned into test_content, and within that a folder
		// 'assignment' will be created,
		// into which the student's submission will be cloned
		// Git authentication will be using the Artemis password, since no token is
		// provided
		// This works for Artemis' new LocalVC, but not necessarily for GitLab (can't
		// test that right now)
		// The try-with-resource deletes the cloned submission from the local filesystem
		// on close
		var submissionPath = Path.of("test_content");
		try (var clonedSubmission = assessment.getSubmission().cloneInto(submissionPath, null)) {
			// Execute the autograder on the cloned submission
			var autograderResult = AutograderRunner.runAutograder(assessment, clonedSubmission, Locale.GERMANY, 0, status -> {
				System.out.println("Autograder Status: " + status);
			});
			System.out.println("Autograder made " + autograderResult.annotationsMade() + " annotations");
		} catch (AutograderFailedException e) {
			System.err.println("Autograder failed: " + e.getMessage());
		}

		// Add a non-custom annotation with a custom message
		assessment.addPredefinedAnnotation(gradingConfig.getMistakeTypeById("complexCode").get(), "src/edu/kit/kastel/StringUtility.java", 12, 13,
				"this is a custom message");
		assertEquals(-0.5, assessment.calculateTotalPointsOfAnnotations());

		// Add a custom annotation
		// The total points will not change, since the grading config specifies that the
		// "comment" rating group cannot deduct points
		assessment.addCustomAnnotation(gradingConfig.getMistakeTypeById("custom").get(), "src/edu/kit/kastel/StringUtility.java", 40, 40, "custom", -1.0);
		assertEquals(-0.5, assessment.calculateTotalPointsOfAnnotations());

		// unnecessaryComplex has a threshold of 5 annotations, so adding 4 doesn't
		// change to total points
		for (int i = 0; i < 4; i++) {
			assessment.addPredefinedAnnotation(gradingConfig.getMistakeTypeById("unnecessaryComplex").get(), "src/edu/kit/kastel/StringUtility.java", 13, 13,
					null);
		}
		assertEquals(-0.5, assessment.calculateTotalPointsOfAnnotations());

		// Adding a fifth annotation will deduct 0.5 points
		assessment.addPredefinedAnnotation(gradingConfig.getMistakeTypeById("unnecessaryComplex").get(), "src/edu/kit/kastel/StringUtility.java", 13, 13, null);
		assertEquals(-1.0, assessment.calculateTotalPointsOfAnnotations());

		// The 'wrongLoopType' is set to be reported, but does not score, so the points
		// do not change
		assessment.addPredefinedAnnotation(gradingConfig.getMistakeTypeById("wrongLoopType").get(), "src/edu/kit/kastel/StringUtility.java", 13, 13, null);
		assertEquals(-1.0, assessment.calculateTotalPointsOfAnnotations());

		// Above, we only checked the points deducted by annotations
		// We can also look at the total points, including tests
		// The submission passed all tests, so the total points are the maximum points
		// minus the points deducted by annotations
		assertEquals(assessment.getMaxPoints() - 1.0, assessment.calculateTotalPoints());

		try {
			// Save & submit the assessment, and translate all messages to German
			// This includes (non-custom) annotation messages, the headers of the feedbacks
			// in Artemis, ...
			assessment.submit(Locale.GERMANY);
		} catch (Exception ex) {
			// Cancel the assessment if anything goes wrong, so that it is not locked
			// indefinitely
			// This is just required for the test, in practice you wouldn't want to delete
			// the assessment
			assessment.cancel();
			throw ex;
		}
	}
}
