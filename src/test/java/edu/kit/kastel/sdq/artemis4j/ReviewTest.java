/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import edu.kit.kastel.sdq.artemis4j.grading.Exam;
import edu.kit.kastel.sdq.artemis4j.grading.ExamExerciseGroup;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmissionWithResults;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import org.junit.jupiter.api.Test;

class ReviewTest {
    private static final String ROUND_ONE_FEEDBACK = "feedback round 1";
    private static final String ROUND_TWO_FEEDBACK = "feedback round 2";

    private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
    private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
    private static final String STUDENT_USER = System.getenv("STUDENT_USER");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
    private static final String COURSE_ID = System.getenv("COURSE_ID");
    private static final String EXAM_ID = System.getenv("EXAM_ID");
    private static final String EXERCISE_GROUP_ID = System.getenv("EXAM_EXERCISE_GROUP_ID");
    private static final String PROGRAMMING_EXERCISE_ID = System.getenv("EXAM_PROGRAMMING_EXERCISE_ID");

    @Test
    void testExamAssessment() throws ArtemisClientException, IOException {
        ArtemisInstance artemis = new ArtemisInstance(ARTEMIS_URL);
        ArtemisConnection connection =
                ArtemisConnection.connectWithUsernamePassword(artemis, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);
        Course course = connection.getCourseById(Integer.parseInt(COURSE_ID));
        Exam exam = course.getExamById(Integer.parseInt(EXAM_ID));
        ExamExerciseGroup exerciseGroup = exam.getExerciseGroupById(Integer.parseInt(EXERCISE_GROUP_ID));
        ProgrammingExercise exercise =
                exerciseGroup.getProgrammingExerciseById(Integer.parseInt(PROGRAMMING_EXERCISE_ID));

        GradingConfig config =
                GradingConfig.readFromString(Files.readString(Path.of("src/test/resources/config.json")), exercise);

        var submission = findSubmission(exercise.fetchAllSubmissions(), STUDENT_USER);

        Assessment roundOneAssessment =
                submission.getFirstRoundAssessment().lockAndOpen(config).orElseThrow();
        roundOneAssessment.clearAnnotations();
        roundOneAssessment.addCustomAnnotation(
                config.getMistakeTypeById("custom"),
                "src/edu/kit/informatik/BubbleSort.java",
                1,
                2,
                ROUND_ONE_FEEDBACK,
                -2.0);
        roundOneAssessment.submit();

        Assessment roundTwoAssessment =
                submission.getSecondRoundAssessment().lockAndOpen(config).orElseThrow();
        roundTwoAssessment.addCustomAnnotation(
                config.getMistakeTypeById("custom"),
                "src/edu/kit/informatik/BubbleSort.java",
                2,
                3,
                ROUND_TWO_FEEDBACK,
                -1.0);
        roundTwoAssessment.submit();

        GradingConfig reviewConfig = GradingConfig.readFromString(
                Files.readString(Path.of("src/test/resources/config_review.json")), exercise);
        Assessment reviewAssessment =
                submission.getReviewAssessment().lockAndOpen(reviewConfig).orElseThrow();
        var annotations = reviewAssessment.getAnnotations(true);
        assertEquals(2, annotations.size());
        assertTrue(annotations.stream()
                .anyMatch(a -> a.getSource() == AnnotationSource.MANUAL_FIRST_ROUND
                        && a.getCustomMessage().equals(Optional.of(ROUND_ONE_FEEDBACK))));
        assertTrue(annotations.stream()
                .anyMatch(a -> a.getSource() == AnnotationSource.MANUAL_SECOND_ROUND
                        && a.getCustomMessage().equals(Optional.of(ROUND_TWO_FEEDBACK))));

        // Assessor tracking
        for (var annotation : annotations) {
            assertEquals(
                    connection.getAssessor().getId(), annotation.getCreatorId().orElseThrow());
        }

        assertEquals(-3.0, reviewAssessment.calculateTotalPointsOfAnnotations());

        // Suppression
        reviewAssessment.suppressAnnotation(annotations.getFirst());
        assertEquals(
                connection.getAssessor().getId(),
                annotations.getFirst().getSuppressorId().orElseThrow());
        assertEquals(-1.0, reviewAssessment.calculateTotalPointsOfAnnotations());
        assertEquals(1, reviewAssessment.getAnnotations().size());
        assertEquals(2, reviewAssessment.getAnnotations(true).size());

        // Unsuppression
        reviewAssessment.unsuppressAnnotation(annotations.getFirst());
        assertEquals(Optional.empty(), annotations.getFirst().getSuppressorId());
        assertEquals(-3.0, reviewAssessment.calculateTotalPointsOfAnnotations());
        assertEquals(2, reviewAssessment.getAnnotations().size());
        assertEquals(2, reviewAssessment.getAnnotations(true).size());

        // Suppress again and submit
        reviewAssessment.suppressAnnotation(annotations.getFirst());
        reviewAssessment.submit();

        // Fetch again and see if the suppression is still there
        reviewAssessment =
                submission.getReviewAssessment().lockAndOpen(reviewConfig).orElseThrow();
        assertEquals(1, reviewAssessment.getAnnotations().size());
        assertEquals(2, reviewAssessment.getAnnotations(true).size());
        annotations = reviewAssessment.getAnnotations(true);
        assertEquals(2, annotations.size());
        assertEquals(
                connection.getAssessor().getId(),
                annotations.getFirst().getSuppressorId().orElseThrow());
        assertEquals(Optional.empty(), annotations.get(1).getSuppressorId());
        assertEquals(-1.0, reviewAssessment.calculateTotalPointsOfAnnotations());
    }

    private ProgrammingSubmissionWithResults findSubmission(
            List<ProgrammingSubmissionWithResults> submissions, String student) {
        return submissions.stream()
                .filter(submission ->
                        submission.getSubmission().getParticipantIdentifier().equals(student))
                .findFirst()
                .orElseThrow();
    }
}
