/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProgrammingExerciseLatestSubmissionTest {

    private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
    private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
    private static final String STUDENT_USER = System.getenv("STUDENT_USER");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
    private static final String COURSE_ID = System.getenv("COURSE_ID");
    private static final String PROGRAMMING_EXERCISE_ID = System.getenv("PROGRAMMING_EXERCISE_ID");

    private ArtemisConnection connection;
    private ProgrammingExercise exercise;
    private long studentId;

    @BeforeAll
    void setup() throws ArtemisClientException {
        Assumptions.assumeTrue(INSTRUCTOR_USER != null && !INSTRUCTOR_USER.isBlank(), "INSTRUCTOR_USER not configured");
        Assumptions.assumeTrue(
                INSTRUCTOR_PASSWORD != null && !INSTRUCTOR_PASSWORD.isBlank(), "INSTRUCTOR_PASSWORD not configured");
        Assumptions.assumeTrue(STUDENT_USER != null && !STUDENT_USER.isBlank(), "STUDENT_USER not configured");
        Assumptions.assumeTrue(ARTEMIS_URL != null && !ARTEMIS_URL.isBlank(), "ARTEMIS_URL not configured");
        Assumptions.assumeTrue(COURSE_ID != null && !COURSE_ID.isBlank(), "COURSE_ID not configured");
        Assumptions.assumeTrue(
                PROGRAMMING_EXERCISE_ID != null && !PROGRAMMING_EXERCISE_ID.isBlank(), "PROGRAMMING_EXERCISE_ID not configured");

        this.connection = ArtemisConnection.connectWithUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);

        long courseId = Long.parseLong(COURSE_ID);
        this.exercise = this.resolveExercise(courseId, Long.parseLong(PROGRAMMING_EXERCISE_ID));
        this.studentId = CourseDTO.fetchAllStudents(this.connection.getClient(), courseId).stream()
                .filter(user -> STUDENT_USER.equals(user.login()))
                .mapToLong(UserDTO::id)
                .findFirst()
                .orElseThrow(() -> new ArtemisClientException("Configured STUDENT_USER is not enrolled in the course"));
    }

    @Test
    void fetchLatestSubmissionWithResultsForUserIdReturnsSubmissionWithResultAndFeedback() throws ArtemisNetworkException {
        var latestSubmission = this.exercise.fetchLatestSubmissionFor(this.studentId);
        Assumptions.assumeTrue(
                latestSubmission.isPresent(), "No latest submission with result available for configured student");

        var submissionWithResults = latestSubmission.orElseThrow();
        assertEquals(STUDENT_USER, submissionWithResults.getSubmission().getParticipantIdentifier());
        assertEquals(submissionWithResults.getSubmission().getCommitHash(), submissionWithResults.getCommitHash());
        assertTrue(submissionWithResults.getCommitHashForLatestResult().isPresent(), "Expected commit hash for latest result");
        assertEquals(
                submissionWithResults.getSubmission().getCommitHash(),
                submissionWithResults.getCommitHashForLatestResult().orElseThrow());

        var latestResult = submissionWithResults.getLatestResult().orElseThrow();
        assertNotNull(latestResult.feedbacks(), "Expected detailed feedback list on latest result");
        assertTrue(latestResult.feedbacks().stream().noneMatch(Objects::isNull), "Feedback list must not contain null entries");
    }

    @Test
    void fetchLatestSubmissionWithResultsForUserDelegatesToUserIdFlow() throws ArtemisNetworkException {
        var submission = this.exercise.fetchLatestSubmissionFor(this.studentId);

        Assumptions.assumeTrue(
                submission.isPresent(), "No latest submission with result available for configured student");
    }

    @Test
    void findUserByIdReturnsCurrentUser() throws ArtemisNetworkException {
        var assessor = this.connection.getAssessor();
        var resolved = this.connection.findUserById(assessor.getId());

        assertTrue(resolved.isPresent());
        assertEquals(assessor.getId(), resolved.orElseThrow().getId());
        assertEquals(assessor.getLogin(), resolved.orElseThrow().getLogin());
    }

    @Test
    void fetchLatestSubmissionWithResultsForUnknownUserIdReturnsEmpty() throws ArtemisNetworkException {
        var latestSubmission = this.exercise.fetchLatestSubmissionFor(Long.MAX_VALUE);
        assertTrue(latestSubmission.isEmpty());
    }

    private ProgrammingExercise resolveExercise(long courseId, long exerciseId) throws ArtemisClientException {
        Course course = this.connection.getCourses().stream()
                .filter(candidate -> candidate.getId() == courseId)
                .findFirst()
                .orElseThrow(() -> new ArtemisClientException("Configured COURSE_ID does not exist or is not visible"));

        return course.getProgrammingExercises().stream()
                .filter(candidate -> candidate.getId() == exerciseId)
                .findFirst()
                .orElseThrow(() -> new ArtemisClientException(
                        "Configured PROGRAMMING_EXERCISE_ID does not exist in the configured course"));
    }
}
