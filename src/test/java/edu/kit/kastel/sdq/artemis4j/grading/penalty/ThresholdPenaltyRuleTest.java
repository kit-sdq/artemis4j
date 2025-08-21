/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;

import edu.kit.kastel.sdq.artemis4j.grading.Annotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ThresholdPenaltyRuleTest {
    private static Annotation makeAnnotation() {
        // for testing purposes, it is enough to return null, because the method only uses the
        // .size() method of the list.
        //
        // In the future one can change this method to return an actual annotation object.
        return null;
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            useHeadersInDisplayName = true,
            value = {
                " Threshold     | Repetitions | Penalty | Annotations | Expected  | IsCapped ",
                " 1             | 1           | 0.5     | 1           | -0.5      | false    ",
                " 1             | 1           | 0.5     | 2           | -0.5      | true     ",
                " 1             | 1           | 0.5     | 3           | -0.5      | true     ",
                " 1             | 1           | 0.5     | 4           | -0.5      | true     ",
                //
                " 2             | 1           | 0.5     | 1           | -0.0      | false    ",
                " 2             | 1           | 0.5     | 2           | -0.5      | false    ",
                " 2             | 1           | 0.5     | 3           | -0.5      | true     ",
                " 2             | 1           | 0.5     | 4           | -0.5      | true     ",
                //
                " 2             | 3           | 0.5     | 1           | -0.0      | false    ",
                " 2             | 3           | 0.5     | 2           | -0.5      | false    ",
                " 2             | 3           | 0.5     | 3           | -0.5      | false    ",
                " 2             | 3           | 0.5     | 4           | -1.0      | false    ",
                " 2             | 3           | 0.5     | 5           | -1.0      | false    ",
                " 2             | 3           | 0.5     | 6           | -1.5      | false    ",
                " 2             | 3           | 0.5     | 7           | -1.5      | true     ",
                " 2             | 3           | 0.5     | 8           | -1.5      | true     ",
                //
                " 5             | 2           | 0.5     | 1           | -0.0      | false    ",
                " 5             | 2           | 0.5     | 2           | -0.0      | false    ",
                " 5             | 2           | 0.5     | 3           | -0.0      | false    ",
                " 5             | 2           | 0.5     | 4           | -0.0      | false    ",
                " 5             | 2           | 0.5     | 5           | -0.5      | false    ",
                " 5             | 2           | 0.5     | 6           | -0.5      | false    ",
                " 5             | 2           | 0.5     | 7           | -0.5      | false    ",
                " 5             | 2           | 0.5     | 8           | -0.5      | false    ",
                " 5             | 2           | 0.5     | 9           | -0.5      | false    ",
                " 5             | 2           | 0.5     | 10          | -1.0      | false    ",
                " 5             | 2           | 0.5     | 11          | -1.0      | true     ",
                " 5             | 2           | 0.5     | 12          | -1.0      | true     ",
            })
    void testCalculatePoints(
            int threshold, Integer repetitions, double penalty, int annotations, double expected, boolean isCapped) {
        PenaltyRule rule = new ThresholdPenaltyRule(threshold, penalty, repetitions == null ? 1 : repetitions);
        Points points = rule.calculatePoints(Collections.nCopies(annotations, makeAnnotation()));

        assertEquals(expected, points.score());
        assertEquals(isCapped, points.capped());
    }
}
