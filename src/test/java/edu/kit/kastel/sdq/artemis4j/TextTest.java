/* Licensed under EPL-2.0 2023-2025. */
package edu.kit.kastel.sdq.artemis4j;

import java.util.List;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.TextBlockDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.TextAnnotation;
import edu.kit.kastel.sdq.artemis4j.grading.TextAssessment;
import edu.kit.kastel.sdq.artemis4j.grading.TextExercise;
import edu.kit.kastel.sdq.artemis4j.grading.TextSubmission;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TextTest {
    // Configure a simple Programming exercise (Sorting algorithms in Artemis
    // (Default Template: Package Name: edu.kit.informatik))
    private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
    private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
    private static final String STUDENT_USER = System.getenv("STUDENT_USER");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
    private static final String COURSE_ID = System.getenv("COURSE_ID");
    private static final String TEXT_EXERCISE_ID = System.getenv("TEXT_EXERCISE_ID");

    private ArtemisInstance artemisInstance;
    private ArtemisConnection connection;
    private Course course;
    private TextAssessment assessment;
    private TextExercise textExercise;
    private TextSubmission textSubmission;

    @BeforeAll
    void checkConfiguration() {
        Assertions.assertNotNull(INSTRUCTOR_USER);
        Assertions.assertNotNull(INSTRUCTOR_PASSWORD);
        Assertions.assertNotNull(STUDENT_USER);
        Assertions.assertNotNull(ARTEMIS_URL);
        Assertions.assertNotNull(COURSE_ID);
        Assertions.assertNotNull(TEXT_EXERCISE_ID);
    }

    @BeforeEach
    void setup() throws ArtemisClientException {
        this.artemisInstance = new ArtemisInstance(ARTEMIS_URL);
        this.connection = ArtemisConnection.connectWithUsernamePassword(
                this.artemisInstance, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);

        this.course = this.connection.getCourses().stream()
                .filter(c -> c.getId() == Integer.parseInt(COURSE_ID))
                .findFirst()
                .orElseThrow();
        this.textExercise = this.course.getTextExercises().stream()
                .filter(e -> e.getId() == Long.parseLong(TEXT_EXERCISE_ID))
                .findFirst()
                .orElseThrow();

        var submissions = this.textExercise.fetchSubmissions(0, false);
        this.textSubmission = submissions.stream()
                .filter(a -> a.getParticipantIdentifier().equals("user01"))
                .findFirst()
                .orElseThrow();

        // ensure that the submission is locked
        this.assessment = this.textSubmission.openAssessment();
        this.assessment.clearAnnotations();

        Assertions.assertTrue(this.assessment.getAnnotations().isEmpty());

        // we can't delete assessments, instead we overwrite them for all submissions, so that they are owned by the
        // same user:
        for (TextSubmission submission : submissions) {
            TextAssessment textAssessment = submission.openAssessment();

            textAssessment.clearAnnotations();
            textAssessment.submit();
        }
    }

    @Test
    void testComputingTextBlockId() {
        // ensure that the text block id is computed correctly
        Assertions.assertEquals(
                "18d529c4bfc668951bfea03eaa5f9842893e991b",
                TextBlockDTO.computeId(696, "Mac OS X 11.03 x86_64", 0, 21));
    }

    @Test
    void testListingTextExercises() throws ArtemisClientException {
        List<TextExercise> textExercises = this.course.getTextExercises();

        for (TextExercise exercise : textExercises) {
            Assertions.assertEquals(this.course, exercise.getCourse());
        }

        Assertions.assertFalse(textExercises.isEmpty(), "It should find at least one text exercise");

        TextExercise firstTextExercise = textExercises.stream()
                .filter(e -> e.getTitle().equals("Textaufgabe 1"))
                .findFirst()
                .orElse(null);
        Assertions.assertNotNull(firstTextExercise, "Text exercise \"Textaufgabe 1\" not found");
    }

    @Test
    void testListingTextSubmissions() throws ArtemisClientException {
        List<TextSubmission> submissions = this.textExercise.fetchSubmissions(0, false);
        Assertions.assertFalse(
                submissions.isEmpty(),
                "No submissions found for text exercise \"%s\"".formatted(this.textExercise.getTitle()));
    }

    @Test
    void testSubmittingAssessment() throws ArtemisClientException {
        this.assessment.addAnnotation(
                2.0, "This is a test feedback", 0, this.assessment.getText().length());

        this.assessment.submit();

        // fetch the assessment again to check if it was submitted:
        TextAssessment artemisAssessment = this.textSubmission.openAssessment();
        Assertions.assertFalse(artemisAssessment.getAnnotations().isEmpty());
        Assertions.assertEquals(
                List.of(new TextAnnotation(
                        2.0,
                        "This is a test feedback",
                        0,
                        artemisAssessment.getText().length())),
                artemisAssessment.getAnnotations());
    }
}
