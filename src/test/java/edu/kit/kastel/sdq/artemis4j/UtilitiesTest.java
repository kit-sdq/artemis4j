/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j;

import java.util.ArrayList;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.client.StudentExamDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class does not contain tests as usual. It is used to perform recurring
 * tasks like toggling all exams to submitted.
 */
@Disabled
public class UtilitiesTest {
    private static final Logger log = LoggerFactory.getLogger(UtilitiesTest.class);

    private static final String hostname = "https://artemis.praktomat.cs.kit.edu";
    private final String username = System.getenv("ARTEMIS_USERNAME");
    private final String password = System.getenv("ARTEMIS_PASSWORD");
    private final String courseId = System.getenv("ARTEMIS_COURSE_ID");
    private final String examId = System.getenv("ARTEMIS_EXAM_ID");

    private ArtemisClient client;

    @BeforeEach
    void setup() throws ArtemisClientException {
        Assertions.assertNotNull(username);
        Assertions.assertNotNull(password);

        ArtemisInstance artemis = new ArtemisInstance(hostname);
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
            try {
                StudentExamDTO.toggleToSubmitted(client, courseId, examId, studentExam.id());
                toggleSucceeded++;
            } catch (ArtemisNetworkException e) {
                log.error("Toggling failed for student " + studentExam.user().login(), e);
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
            var submissions = new ArrayList<>(ProgrammingSubmissionDTO.fetchAll(client, exercise.id(), 0, false));
            if (exercise.secondCorrectionEnabled()) {
                submissions.addAll(ProgrammingSubmissionDTO.fetchAll(client, exercise.id(), 1, false));
            }

            for (var submission : submissions) {
                var latestResult = submission.results().get(submission.results().size() - 1);
                if (latestResult == null) {
                    log.warn("No result for submission " + submission.id());
                    continue;
                }

                boolean mandatoryFailed = latestResult.score() == 0.0;
                if (mandatoryFailed) {
                    log.info("Student " + submission.participation().participantIdentifier()
                            + " failed mandatory tests");
                    var newResult = ResultDTO.forAssessmentSubmission(
                            submission.id(), 0.0, latestResult.feedbacks(), latestResult.assessor());
                    ProgrammingSubmissionDTO.saveAssessment(
                            client, submission.participation().id(), true, newResult);
                }
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
}
