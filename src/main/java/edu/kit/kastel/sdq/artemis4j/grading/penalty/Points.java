/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

public record Points(double score, boolean capped) {
    public Points {
        if (score == -0.0) {
            score = 0.0;
        }
    }
}
