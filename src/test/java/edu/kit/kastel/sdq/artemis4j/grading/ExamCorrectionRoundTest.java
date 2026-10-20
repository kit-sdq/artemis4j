/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ExamDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExamCorrectionRoundTest {
    @ParameterizedTest
    @CsvSource({"1, false", "2, true"})
    void propagatesExamCorrectionRoundsToProgrammingExercises(int rounds, boolean secondRoundEnabled) throws Exception {
        var dto = ArtemisClient.MAPPER.readValue("""
                {
                    "id": 1,
                    "numberOfCorrectionRoundsInExam": %d,
                    "exerciseGroups": [{
                        "id": 2,
                        "exercises": [{"id": 3, "type": "programming", "title": "Exercise", "maxPoints": 10}]
                    }]
                }
                """.formatted(rounds), ExamDTO.class);
        var courseDto = ArtemisClient.MAPPER.readValue("""
                {"id": 4, "title": "Course", "shortName": "course"}
                """, CourseDTO.class);
        var connection = ArtemisConnection.fromToken(new ArtemisInstance("https://example.invalid"), "unused");
        var exam = new Exam(dto, new Course(courseDto, connection));
        var exercise = exam.getExerciseGroupById(2).getProgrammingExerciseById(3);

        assertEquals(secondRoundEnabled, dto.hasSecondCorrectionRound());
        assertEquals(secondRoundEnabled, exam.hasSecondCorrectionRound());
        assertEquals(secondRoundEnabled, exercise.hasSecondCorrectionRound());
    }
}
