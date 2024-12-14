/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;

public final class ThresholdPenaltyRule implements PenaltyRule {
    private final int threshold;
    private final double penalty;
    private final int repetitions;

    @JsonCreator
    public ThresholdPenaltyRule(
            @JsonProperty("threshold") int threshold,
            @JsonProperty(value = "penalty", required = true) double penalty,
            @JsonProperty(value = "repetitions", defaultValue = "1") int repetitions) {
        this.threshold = threshold;
        // It is not defined how the code should behave if the threshold is 0 or negative, therefore an exception is
        // thrown here.
        if (this.threshold <= 0) {
            throw new IllegalArgumentException("The threshold must be at least 1, but was %d".formatted(threshold));
        }
        this.penalty = penalty;
        // sanity check in case this is misused.
        if (repetitions < 1) {
            throw new IllegalArgumentException(
                    "If specified, the repetitions must be at least 1, but was %d".formatted(repetitions));
        }
        this.repetitions = repetitions;
    }

    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        // The repetitions are used to allow a threshold penalty to be applied multiple times.
        // For example with threshold 2, penalty 0.5 and repetitions 2 and 10 annotations, it would:
        // 1. "ignore" the first annotation
        // 2. annotation 2 would result in a penalty of 0.5
        // 3. annotation 4 would result in a penalty of 0.5 (2nd time)
        // any further annotations would not be penalized.

        return new Points(
                Math.min(annotations.size() / this.threshold, this.repetitions) * -this.penalty,
                annotations.size() > this.threshold * this.repetitions);
    }

    public int getThreshold() {
        return threshold;
    }
}
