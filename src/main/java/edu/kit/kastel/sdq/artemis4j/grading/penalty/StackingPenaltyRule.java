/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;
import org.jspecify.annotations.Nullable;

public final class StackingPenaltyRule implements PenaltyRule {
    private final double penalty;
    private final @Nullable Integer maxUses;

    @JsonCreator
    public StackingPenaltyRule(
            @JsonProperty(value = "penalty", required = true) double penalty,
            @JsonProperty("maxUses") @Nullable Integer maxUses) {
        this.penalty = penalty;
        this.maxUses = maxUses;
    }

    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        int multiplier = this.maxUses == null ? annotations.size() : Math.min(annotations.size(), this.maxUses);
        double penaltyPoints = multiplier * -this.penalty;
        return new Points(penaltyPoints, this.maxUses != null && annotations.size() > this.maxUses);
    }
}
