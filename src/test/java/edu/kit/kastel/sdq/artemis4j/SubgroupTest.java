/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Assessment;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmissionWithResults;
import edu.kit.kastel.sdq.artemis4j.grading.location.Location;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.InvalidGradingConfigException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.MistakeType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * This tests the subgroup feature which can be used to define subgroups of rating groups.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubgroupTest {

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
    private ProgrammingSubmissionWithResults programmingSubmission;
    private Assessment assessment;
    private GradingConfig gradingConfig;

    @BeforeAll
    void checkConfiguration() {
        Assertions.assertNotNull(INSTRUCTOR_USER);
        Assertions.assertNotNull(INSTRUCTOR_PASSWORD);
        Assertions.assertNotNull(STUDENT_USER);
        Assertions.assertNotNull(ARTEMIS_URL);
        Assertions.assertNotNull(COURSE_ID);
        Assertions.assertNotNull(PROGRAMMING_EXERCISE_ID);
    }

    @BeforeEach
    void setup() throws ArtemisClientException, IOException {
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

        var submissions = this.exercise.fetchAllSubmissions();
        this.programmingSubmission = submissions.stream()
                .filter(s -> s.getSubmission().getParticipantIdentifier().equals(STUDENT_USER))
                .findFirst()
                .orElseThrow();

        this.gradingConfig = GradingConfig.readFromString(
                Files.readString(Path.of("src/test/resources/config.json")), this.exercise);

        // ensure that the submission is locked
        this.assessment = this.programmingSubmission
                .getFirstRoundAssessment()
                .lockAndOpen(this.gradingConfig)
                .orElseThrow();
        this.assessment.clearAnnotations();

        Assertions.assertTrue(this.assessment.getAllAnnotations().isEmpty());
    }

    /**
     * Tests that a subgroup without a parent group is not allowed.
     */
    @Test
    void testSubgroupWithoutParent() {
        var configString =
                """
                        {
                            "shortName": "E2E",
                            "positiveFeedbackAllowed": true,
                            "ratingGroups": [
                                {
                                    "shortName": "functionality::quality",
                                    "displayName": "Code Qualität",
                                    "additionalDisplayNames": {
                                        "en": "Code Quality"
                                    },
                                    "negativeLimit": -3,
                                    "positiveLimit": null
                                }
                            ],
                            "mistakeTypes": [
                                {
                                    "shortName": "unused",
                                    "button": "Unused Element",
                                    "message": "Das Element wird nicht verwendet",
                                    "penaltyRule": {
                                        "shortName": "thresholdPenalty",
                                        "threshold": 1,
                                        "penalty": 1.0
                                    },
                                    "appliesTo": "functionality::quality"
                                }
                            ]
                        }

                        """;

        assertThrowsExactly(
                InvalidGradingConfigException.class,
                () -> GradingConfig.readFromString(configString, this.exercise),
                "Subgroup 'functionality::quality' has no parent group with id 'functionality'");
    }

    /**
     * This test checks that nested subgroups like "functionality::quality::test" are forbidden.
     * This is a temporary restriction that could be removed in the future.
     */
    @Test
    void testNestedSubgroup() {
        var configString =
                """
                        {
                            "shortName": "E2E",
                            "positiveFeedbackAllowed": true,
                            "ratingGroups": [
                                {
                                    "shortName": "functionality",
                                    "displayName": "Code Qualität",
                                    "additionalDisplayNames": {
                                        "en": "Code Quality"
                                    },
                                    "negativeLimit": -3,
                                    "positiveLimit": null
                                },
                                {
                                    "shortName": "functionality::quality",
                                    "displayName": "Code Qualität",
                                    "additionalDisplayNames": {
                                        "en": "Code Quality"
                                    },
                                    "negativeLimit": -3,
                                    "positiveLimit": null
                                },
                                {
                                    "shortName": "functionality::quality::extra",
                                    "displayName": "Code Qualität",
                                    "additionalDisplayNames": {
                                        "en": "Code Quality"
                                    },
                                    "negativeLimit": -3,
                                    "positiveLimit": null
                                }
                            ],
                            "mistakeTypes": [
                                {
                                    "shortName": "unused",
                                    "button": "Unused Element",
                                    "message": "Das Element wird nicht verwendet",
                                    "penaltyRule": {
                                        "shortName": "thresholdPenalty",
                                        "threshold": 1,
                                        "penalty": 1.0
                                    },
                                    "appliesTo": "functionality::quality"
                                }
                            ]
                        }

                        """;

        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> GradingConfig.readFromString(configString, this.exercise),
                "Subgroup functionality::quality::extra is a nested subgroup");
    }

    private List<String> getGlobalFeedbackLines() throws ArtemisClientException {
        this.assessment.submit();
        // after submitting, we need to check that the global feedback looks as expected
        this.assessment = this.programmingSubmission
                .getFirstRoundAssessment()
                .lockAndOpen(this.gradingConfig)
                .orElseThrow();

        ResultDTO resultDTO =
                this.programmingSubmission.getFirstRoundAssessment().result();
        var feedbacks = ResultDTO.fetchDetailedFeedbacks(
                this.connection.getClient(),
                resultDTO.id(),
                this.programmingSubmission.getSubmission().getParticipationId(),
                resultDTO.feedbacks());

        List<String> globalFeedbackLines = new ArrayList<>();
        for (FeedbackDTO feedbackDTO : feedbacks) {
            if (feedbackDTO.type() != FeedbackType.MANUAL_UNREFERENCED || feedbackDTO.visibility() != null) {
                continue;
            }

            globalFeedbackLines.addAll(Arrays.asList(feedbackDTO.detailText().split("\\n")));
        }

        return globalFeedbackLines;
    }

    /**
     * Tests that the limit defined in the subgroup is respected.
     *
     * @throws ArtemisClientException if there are problems with artemis
     */
    @Test
    void testSubgroupLimitNotExceeded() throws ArtemisClientException {
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("systemexit");

        this.assessment.addPredefinedAnnotation(
                mistakeType, new Location("src/edu/kit/informatik/BubbleSort.java", 0, 1), null);
        this.assessment.addPredefinedAnnotation(
                this.gradingConfig.getMistakeTypeById("magicLiteral"),
                new Location("src/edu/kit/informatik/BubbleSort.java", 1, 2),
                null);

        assertEquals(
                List.of(
                        "Funktionalität [-4P (Range: -20P -- ∞P)]",
                        "    * System.exit [-5P]:",
                        "        * src/edu/kit/informatik/BubbleSort.java at line 1",
                        "    * Magic Literal [-1P]:",
                        "        * src/edu/kit/informatik/BubbleSort.java at line 2"),
                this.getGlobalFeedbackLines());
    }

    /**
     * Tests that the limit defined in the subgroup is independent of other subgroups.
     *
     * @throws ArtemisClientException if there are problems with artemis
     */
    @Test
    void testOtherSubgroupsAreIndependent() throws ArtemisClientException {
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("systemexit");

        this.assessment.addPredefinedAnnotation(
                mistakeType, new Location("src/edu/kit/informatik/BubbleSort.java", 0, 1), null);
        this.assessment.addPredefinedAnnotation(
                this.gradingConfig.getMistakeTypeById("magicLiteral"),
                new Location("src/edu/kit/informatik/BubbleSort.java", 1, 2),
                null);
        this.assessment.addPredefinedAnnotation(
                this.gradingConfig.getMistakeTypeById("instanceof"),
                new Location("src/edu/kit/informatik/BubbleSort.java", 3, 4),
                null);
        this.assessment.addPredefinedAnnotation(
                this.gradingConfig.getMistakeTypeById("instanceof"),
                new Location("src/edu/kit/informatik/BubbleSort.java", 4, 5),
                null);

        assertEquals(
                List.of(
                        "Funktionalität [-14P (Range: -20P -- ∞P)]",
                        "    * System.exit [-5P]:",
                        "        * src/edu/kit/informatik/BubbleSort.java at line 1",
                        "    * Magic Literal [-1P]:",
                        "        * src/edu/kit/informatik/BubbleSort.java at line 2",
                        "    * instanceof [-10P]:",
                        "        * src/edu/kit/informatik/BubbleSort.java at lines 4, 5"),
                this.getGlobalFeedbackLines());
    }

    @Test
    void testSubgroupsAreGroupedCorrectly() throws ArtemisClientException {
        // This test checks that the annotations are merged and displayed correctly for the student.
        MistakeType mistakeType = this.gradingConfig.getMistakeTypeById("visibility");
        MistakeType nonCustomMistakeType = this.gradingConfig.getMistakeTypeById("unused");

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
        this.assessment = this.programmingSubmission
                .getFirstRoundAssessment()
                .lockAndOpen(this.gradingConfig)
                .orElseThrow();

        // so we need to check the submission itself:

        ResultDTO resultDTO =
                this.programmingSubmission.getFirstRoundAssessment().result();
        var feedbacks = ResultDTO.fetchDetailedFeedbacks(
                this.connection.getClient(),
                resultDTO.id(),
                this.programmingSubmission.getSubmission().getParticipationId(),
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
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: Other Feedback 0",
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: Other Feedback 1",
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: Other Feedback 2. Weitere Probleme in Client:(L1, L2, L3, L4, L5), MergeSort:(L4, L5).",
                        // all feedbacks in the same file
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: This is annotation 0",
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: This is annotation 1",
                        "[Funktionalität:Sichtbarkeit] Die Sichtbarkeit ist nicht korrekt\nExplanation: This is annotation 2. Weitere Probleme in L4, L5, L6, L7, L8, L9, L10.",
                        // feedbacks without messages:
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet",
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet",
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet\nExplanation: Weitere Probleme in L9.",
                        // feedbacks where only the last has a message:
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet",
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet",
                        "[Funktionalität:Unused Element] Das Element wird nicht verwendet\nExplanation: Has used last annotation for message. Weitere Probleme in L12."),
                feedbackTexts);
    }
}
