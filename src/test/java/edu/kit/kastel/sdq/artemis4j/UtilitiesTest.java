/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.AssessmentResult;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Feedback;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;
import edu.kit.kastel.sdq.artemis4j.client.RestClientManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * This class does not contain tests as usual. It is used to perform recurring
 * tasks like toggling all exams to submitted.
 */
@Disabled
class UtilitiesTest {

	private static final String hostname = "https://artemis.praktomat.cs.kit.edu";
	private final String username = System.getenv("ARTEMIS_USERNAME");
	private final String password = System.getenv("ARTEMIS_PASSWORD");
	private final String courseId = System.getenv("ARTEMIS_COURSE_ID");
	private final String examId = System.getenv("ARTEMIS_EXAM_ID");

	@Test
	void toggleExams() throws ArtemisClientException {
		Assertions.assertNotNull(username);
		Assertions.assertNotNull(password);
		Assertions.assertNotNull(courseId);
		Assertions.assertNotNull(examId);

		RestClientManager client = new RestClientManager(hostname, username, password);
		client.login();

		var course = client.getCourseArtemisClient().getCourses().stream().filter(c -> String.valueOf(c.getCourseId()).equals(courseId)).findFirst()
				.orElseThrow();
		var exam = course.getExams().stream().filter(e -> String.valueOf(e.getExamId()).equals(examId)).findFirst().orElseThrow();

		var result = client.getExamArtemisClient().markAllExamsAsSubmitted(course, exam);
		System.out.println("All exams: " + result.exams().size());
		System.out.println("Toggle successful: " + result.toggleSuccessful().size());
		System.out.println("Toggle failed:\n" + String.join("\n", result.toggleFailed().stream().map(it -> it.getStudent().getLogin()).toList()));
	}

	@Test
	void markMandatoryFailedAsFailed() throws ArtemisClientException {
		Assertions.assertNotNull(username);
		Assertions.assertNotNull(password);
		Assertions.assertNotNull(courseId);
		Assertions.assertNotNull(examId);

		RestClientManager client = new RestClientManager(hostname, username, password);
		client.login();

		var course = client.getCourseArtemisClient().getCourses().stream().filter(c -> String.valueOf(c.getCourseId()).equals(courseId)).findFirst()
				.orElseThrow();
		var exam = course.getExams().stream().filter(e -> String.valueOf(e.getExamId()).equals(examId)).findFirst().orElseThrow();

		for (var exercise : exam.getExerciseGroups().stream().flatMap(it -> it.getExercises().stream()).toList()) {
			System.out.println("Exercise: " + exercise.getTitle());
			List<Submission> submissionsRound0 = client.getSubmissionArtemisClient().getSubmissions(exercise, 0);
			List<Submission> submissionsRound1 = exam.hasSecondCorrectionRound() ? client.getSubmissionArtemisClient().getSubmissions(exercise, 1) : List.of();

			List<Submission> submissions = new ArrayList<>();
			submissions.addAll(submissionsRound0);
			submissions.addAll(submissionsRound1);

			for (var submission : submissions) {
				var latestResult = submission.getLatestResult();
				if (latestResult == null) {
					System.err.println("No result for submission " + submission.getSubmissionId());
					continue;
				}
				boolean mandatoryFailed = latestResult.score == 0;
				if (mandatoryFailed) {
					System.out.println("Student " + submission.getParticipantIdentifier() + " failed mandatory tests");
					var assessment = client.getAssessmentArtemisClient().startAssessment(submission);
					final List<Feedback> tests = assessment.getLatestFeedback().stream().filter(f -> f.getReference() == null).toList();
					int codeIssueCount = (int) assessment.getLatestFeedback().stream().filter(Feedback::isStaticCodeAnalysis).count();
					int passedTestCaseCount = (int) tests.stream().filter(feedback -> feedback.getPositive() != null && feedback.getPositive()).count();
					AssessmentResult assessmentResult = new AssessmentResult(assessment.getSubmissionId(), "SEMI_AUTOMATIC", 0, true, true,
							client.getAuthenticationClient().getUser(), assessment.getLatestFeedback(), codeIssueCount, passedTestCaseCount, tests.size());
					client.getAssessmentArtemisClient().saveAssessment(assessment.getParticipationId(), true, assessmentResult);
				}
			}

		}

	}
}
