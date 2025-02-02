/* Licensed under EPL-2.0 2023-2025. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.Location;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;
import edu.kit.kastel.sdq.artemis4j.grading.TestResult;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class End2EndTest {

    private static final String FEEDBACK_TEXT =
            "This is a test feedback. To make it very long, it will just repeated over and over again. "
                    .repeat(30)
                    .trim();

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
        this.connection = ArtemisConnection.connectWithUsernamePassword(
                this.artemisInstance, INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);

        this.course = this.connection.getCourses().stream()
                .filter(c -> c.getId() == Integer.parseInt(COURSE_ID))
                .findFirst()
                .orElseThrow();
        this.exercise = this.course.getProgrammingExercises().stream()
                .filter(e -> e.getId() == Long.parseLong(PROGRAMMING_EXERCISE_ID))
                .findFirst()
                .orElseThrow();

        var submissions = this.exercise.fetchSubmissions();
        this.programmingSubmission = submissions.stream()
                .filter(a -> a.getParticipantIdentifier().equals(STUDENT_USER))
                .findFirst()
                .orElseThrow();

        this.gradingConfig = GradingConfig.readFromString(
                Files.readString(Path.of("src/test/resources/config.json")), this.exercise);

        // ensure that the submission is locked
        this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();
        this.assessment.clearAnnotations();

        Assertions.assertTrue(this.assessment.getAnnotations().isEmpty());
    }

    @Test
    void testStats() throws ArtemisClientException {
        var stats = this.exercise.fetchAssessmentStats();
        Assertions.assertNotNull(stats);
    }

    @Test
    void testCreationOfSimpleAnnotations() throws ArtemisClientException {
        MistakeType mistakeType = this.gradingConfig.getMistakeTypes().get(1);

        this.assessment.addPredefinedAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", 1, 2, null);

        this.assessment.submit();

        // Check Assessments
        this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

        List<TestResult> tests = this.assessment.getTestResults();
        assertEquals(13, tests.size());

        assertEquals(1, this.assessment.getAnnotations().size());
        assertEquals(
                AnnotationSource.MANUAL_FIRST_ROUND,
                this.assessment.getAnnotations().get(0).getSource());
    }

    @Test
    void testCreationOfCustomAnnotation() throws ArtemisClientException {
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("custom");

        this.assessment.addCustomAnnotation(
                mistakeType, "src/edu/kit/informatik/BubbleSort.java", 1, 2, FEEDBACK_TEXT, -2.0);
        this.assessment.submit();

        // Check Assessments
        this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

        List<TestResult> tests = this.assessment.getTestResults();
        assertEquals(13, tests.size());

        assertEquals(1, this.assessment.getAnnotations().size());
        var annotation = this.assessment.getAnnotations().get(0);
        assertEquals(AnnotationSource.MANUAL_FIRST_ROUND, annotation.getSource());
        assertEquals(Optional.of(-2.0), annotation.getCustomScore());
        assertEquals(Optional.of(FEEDBACK_TEXT), annotation.getCustomMessage());
    }

    @Test
    void testExportImport() throws ArtemisClientException {
        MistakeType mistakeType = this.gradingConfig.getMistakeTypes().get(1);

        this.assessment.addPredefinedAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", 1, 2, null);
        assertEquals(1, this.assessment.getAnnotations().size());
        var oldAnnotations = this.assessment.getAnnotations();

        String exportedAssessment = this.assessment.exportAssessment();

        this.assessment.clearAnnotations();
        assertEquals(0, this.assessment.getAnnotations().size());

        this.assessment.importAssessment(exportedAssessment);
        assertEquals(oldAnnotations, this.assessment.getAnnotations());
    }

    @Test
    void testAssessmentFetchesFeedbacks() throws ArtemisClientException {
        // This test fetches all submissions of an exercise and checks that the
        // assessment
        // contains the previously added feedback.

        // Create an annotation (feedback) in the submission:
        MistakeType mistakeType = this.gradingConfig.getMistakeTypes().get(1);

        this.assessment.addPredefinedAnnotation(mistakeType, "src/edu/kit/informatik/BubbleSort.java", 1, 2, null);
        this.assessment.submit();

        ProgrammingSubmission updatedSubmission = null;
        // find the programming submission that was just assessed in all submissions of
        // the exercise:
        for (ProgrammingSubmission submission : this.exercise.fetchSubmissions()) {
            if (submission.getId() == this.programmingSubmission.getId()) {
                updatedSubmission = programmingSubmission;
                break;
            }
        }

        assertEquals(this.programmingSubmission, updatedSubmission);

        Assessment newAssessment =
                updatedSubmission.openAssessment(this.gradingConfig).orElseThrow();
        assertEquals(1, newAssessment.getAnnotations().size());
    }

    @Test
    void testCloningViaVCSToken() throws ArtemisClientException {
        var targetPath = Path.of("cloned_code");

        try (var clonedSubmission = this.assessment.getSubmission().cloneViaVCSTokenInto(targetPath, null)) {
            Assertions.assertTrue(Files.exists(targetPath));
        }
        Assertions.assertFalse(Files.exists(targetPath));
    }

    @Test
    void testMergingNotObservableInAssessments() throws ArtemisClientException {
        // This test checks that the annotation merging is not observable.
        // The annotations are created separately, then merged before submission and when the assessment is reloaded,
        // the annotations will still be there (one should not be able to see the merged annotations).
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("custom");

        String defaultFeedbackText = "This is annotation %d";
        for (int i = 0; i < 10; i++) {
            // NOTE: the file has 16 lines, so the annotations are created in a way that they don't overlap
            this.assessment.addAutograderAnnotation(
                    mistakeType,
                    new Location("src/edu/kit/informatik/BubbleSort.java", i, i),
                    defaultFeedbackText.formatted(i),
                    "FirstCheck",
                    "FIRST_PROBLEM_TYPE",
                    3);
        }

        // create a copy of all annotations before submitting (which will merge them)
        List<Annotation> currentAnnotations = new ArrayList<>(this.assessment.getAnnotations());
        this.assessment.submit();

        // Check Assessments
        this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

        for (Annotation annotation : this.assessment.getAnnotations()) {
            Assertions.assertTrue(
                    currentAnnotations.contains(annotation),
                    "Annotation \"%s\" is missing after submission".formatted(annotation));
            currentAnnotations.remove(annotation);
        }

        Assertions.assertTrue(
                currentAnnotations.isEmpty(),
                "There are annotations that were lost after submission: %s".formatted(currentAnnotations));
    }

    @Test
    void testAnnotationMerging() throws ArtemisClientException {
        // This test checks that the annotations are merged and displayed correctly for the student.
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("custom");
        MistakeType nonCustomMistakeType = this.gradingConfig.getMistakeTypes().get(1);

        String defaultFeedbackText = "This is annotation %d";
        for (int i = 0; i < 10; i++) {
            // NOTE: the file has 16 lines, so the annotations are created in a way that they don't overlap
            this.assessment.addAutograderAnnotation(
                    mistakeType,
                    new Location("src/edu/kit/informatik/BubbleSort.java", i, i),
                    defaultFeedbackText.formatted(i),
                    "FirstCheck",
                    "FIRST_PROBLEM_TYPE",
                    3);
        }

        String otherFeedbackText = "Other Feedback %d";
        for (int i = 0; i < 5; i++) {
            this.assessment.addAutograderAnnotation(
                    mistakeType,
                    new Location("src/edu/kit/informatik/MergeSort.java", i, i),
                    otherFeedbackText.formatted(i),
                    "FirstCheck",
                    "SECOND_PROBLEM_TYPE",
                    3);
        }

        // start at line 0 and end at line 5 (L1-6)
        for (int i = 0; i < 5; i++) {
            this.assessment.addAutograderAnnotation(
                    mistakeType,
                    new Location("src/edu/kit/informatik/Client.java", i, i),
                    otherFeedbackText.formatted(i),
                    "FirstCheck",
                    "SECOND_PROBLEM_TYPE",
                    3);
        }

        // add four annotations without custom messages:
        for (int i = 5; i < 9; i++) {
            this.assessment.addAutograderAnnotation(
                    nonCustomMistakeType,
                    new Location("src/edu/kit/informatik/Client.java", i, i),
                    null,
                    "SecondCheck",
                    "THIRD_PROBLEM_TYPE",
                    3);
        }

        // add four annotations where only the last has a custom message:
        for (int i = 9; i < 12; i++) {
            this.assessment.addAutograderAnnotation(
                    nonCustomMistakeType,
                    new Location("src/edu/kit/informatik/Client.java", i, i),
                    null,
                    "ThirdCheck",
                    "THIRD_PROBLEM_TYPE",
                    3);
        }

        this.assessment.addAutograderAnnotation(
                nonCustomMistakeType,
                new Location("src/edu/kit/informatik/Client.java", 13, 13),
                "Has used last annotation for message",
                "ThirdCheck",
                "THIRD_PROBLEM_TYPE",
                3);

        // submit the assessment (will merge the annotations)
        this.assessment.submit();

        // the assessment will not show the merged annotations (it will unmerge them after loading)
        this.assessment = this.programmingSubmission.tryLock(this.gradingConfig).orElseThrow();

        // so we need to check the submission itself:

        ResultDTO resultDTO = this.programmingSubmission.getRelevantResult().orElseThrow();
        var feedbacks = ResultDTO.fetchDetailedFeedbacks(
                this.connection.getClient(),
                resultDTO.id(),
                this.programmingSubmission.getParticipationId(),
                resultDTO.feedbacks());

        List<String> feedbackTexts = new ArrayList<>();
        for (FeedbackDTO feedbackDTO : feedbacks) {
            if (feedbackDTO.type() != FeedbackType.MANUAL) {
                continue;
            }

            feedbackTexts.add(feedbackDTO.detailText());
        }

        assertEquals(
                List.of(
                        // other feedback is 5 annotations in MergeSort and 5 in Client that should be merged
                        "[Funktionalität:Custom Penalty] Other Feedback 0 (0P)",
                        "[Funktionalität:Custom Penalty] Other Feedback 1 (0P)",
                        "[Funktionalität:Custom Penalty] Other Feedback 2. Weitere Probleme in Client:(L1, L2, L3, L4, L5), MergeSort:(L4, L5). (0P)",
                        // all feedbacks in the same file
                        "[Funktionalität:Custom Penalty] This is annotation 0 (0P)",
                        "[Funktionalität:Custom Penalty] This is annotation 1 (0P)",
                        "[Funktionalität:Custom Penalty] This is annotation 2. Weitere Probleme in L4, L5, L6, L7, L8, L9, L10. (0P)",
                        // feedbacks without messages:
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden",
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden",
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden\nExplanation: Weitere Probleme in L9.",
                        // feedbacks where only the last has a message:
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden",
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden",
                        "[Funktionalität:JavaDoc Leer] JavaDoc ist leer oder nicht vorhanden\nExplanation: Has used last annotation for message. Weitere Probleme in L12."),
                feedbackTexts);
    }

    @Test
    void testHighlight() {
        assertEquals(
                MistakeType.Highlight.DEFAULT,
                this.gradingConfig.getMistakeTypeById("custom").getHighlight());
        assertEquals(
                MistakeType.Highlight.DEFAULT,
                this.gradingConfig.getMistakeTypeById("jdEmpty").getHighlight());
        assertEquals(
                MistakeType.Highlight.NONE,
                this.gradingConfig.getMistakeTypeById("magicLiteral").getHighlight());
    }
}
