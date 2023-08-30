/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.client.RestClientManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

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
}
