/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;

public final class ThresholdPenaltyRule implements PenaltyRule {
    private final int threshold;
    private final double penalty;

    @JsonCreator
    public ThresholdPenaltyRule(
            @JsonProperty("threshold") int threshold,
            @JsonProperty(value = "penalty", required = true) double penalty) {
        this.threshold = threshold;
        this.penalty = penalty;
    }

    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        if (annotations.size() >= this.threshold) {
            return new Points(-this.penalty, annotations.size() > this.threshold);
        }
        return new Points(0.0, false);
    }

    public int getThreshold() {
        return threshold;
    }
}
