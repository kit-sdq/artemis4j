/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.client.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class does not contain tests as usual. It is used to perform recurring
 * tasks like toggling all exams to "submitted".
 */
@Disabled
@SuppressWarnings("java:S1117") // Override names that are fields.
class UtilitiesTest {
    private static final Logger log = LoggerFactory.getLogger(UtilitiesTest.class);

    private static final String HOSTNAME = "https://artemis.cs.kit.edu";
    private final String username = System.getenv("ARTEMIS_USERNAME");
    private final String password = System.getenv("ARTEMIS_PASSWORD");
    private final String courseId = System.getenv("ARTEMIS_COURSE_ID");
    private final String examId = System.getenv("ARTEMIS_EXAM_ID");

    private ArtemisClient client;

    @BeforeEach
    void setup() throws ArtemisClientException {
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);

        ArtemisInstance artemis = new ArtemisInstance(HOSTNAME);
        this.client = ArtemisClient.fromUsernamePassword(artemis, username, password);
    }

    @Test
    void toggleExams() throws ArtemisClientException {
        Assertions.assertNotNull(courseId);
        int courseId = Integer.parseInt(this.courseId);
        Assertions.assertNotNull(examId);
        long examId = Long.parseLong(this.examId);

        var studentExams = StudentExamDTO.fetchAll(client, courseId, examId);
        System.out.println("All exams: " + studentExams.size());

        int toggleSucceeded = 0;
        List<String> toggleFailed = new ArrayList<>();
        for (var studentExam : studentExams) {
            if (studentExam.submitted()) {
                continue;
            }
            try {
                StudentExamDTO.toggleToSubmitted(client, courseId, examId, studentExam.id());
                toggleSucceeded++;
            } catch (ArtemisNetworkException e) {
                log.error("Toggling failed for student {}", studentExam.user().login(), e);
                toggleFailed.add(studentExam.user().login());
            }
        }

        System.out.println("Toggle successful: " + toggleSucceeded);
        System.out.println("Toggle failed:\n" + String.join("\n", toggleFailed));
    }

    @Test
    void markMandatoryFailedAsFailed() throws ArtemisClientException {
        Assertions.assertNotNull(courseId);
        int courseId = Integer.parseInt(this.courseId);
        Assertions.assertNotNull(examId);
        long examId = Long.parseLong(this.examId);

        var exam = ExamDTO.fetch(client, courseId, examId);
        var exercises = exam.exerciseGroups().stream()
                .flatMap(e -> e.exercises().stream())
                .toList();
        for (var exercise : exercises) {
            System.out.println("Exercise: " + exercise.title());
            markMandatoryFailedAsFailedForCorrectionRound(exercise, 0);
            if (exercise.secondCorrectionEnabled()) {
                markMandatoryFailedAsFailedForCorrectionRound(exercise, 1);
            }
        }
    }

    private void markMandatoryFailedAsFailedForCorrectionRound(ProgrammingExerciseDTO exercise, int correctionRound)
            throws ArtemisNetworkException {
        var submissions =
                new ArrayList<>(ProgrammingSubmissionDTO.fetchAll(client, exercise.id(), correctionRound, false));
        for (var submission : submissions) {
            var latestResult = submission.results().get(submission.results().size() - 1);
            if (latestResult == null) {
                log.warn("No result for submission {}", submission.id());
                continue;
            }

            boolean mandatoryFailed = latestResult.score() == 0.0;
            if (mandatoryFailed) {
                log.info(
                        "Student {} failed mandatory tests",
                        submission.participation().participantIdentifier());
                var lockingResult = ProgrammingSubmissionDTO.lock(client, submission.id(), correctionRound)
                        .results()
                        .get(0);
                var newResult = ResultDTO.forAssessmentSubmission(
                        submission.id(), 0.0, lockingResult.feedbacks(), lockingResult);
                ProgrammingSubmissionDTO.saveAssessment(
                        client, submission.participation().id(), true, newResult);
            }
        }
    }

    @Test
    void removeTAsFromCourse() throws ArtemisClientException {
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);
        Assertions.assertNotNull(courseId);
        int courseId = Integer.parseInt(this.courseId);

        var tutors = CourseDTO.fetchAllTutors(client, courseId);
        for (var tutor : tutors) {
            CourseDTO.removeTutor(client, courseId, tutor.login());
        }
    }

    @Test
    void removeUnenrolledUsers() throws ArtemisClientException {
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);

        var unenrolledUsers = UserDTO.getUnenrolledUsers(client);
        System.out.println("Unenrolled users: " + unenrolledUsers.size());
        for (var user : unenrolledUsers) {
            System.out.println("Deleting user: " + user);
            UserDTO.deleteUser(client, user);
        }
    }
}
