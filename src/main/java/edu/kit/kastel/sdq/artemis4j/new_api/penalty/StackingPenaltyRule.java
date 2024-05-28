package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;

import java.util.List;

public final class StackingPenaltyRule implements PenaltyRule {
    private final double penalty;
    private final Integer maxUses;

    @JsonCreator
    public StackingPenaltyRule(
            @JsonProperty(value = "penalty", required = true) int penalty,
                               @JsonProperty("maxUses") Integer maxUses
    ) {
        this.penalty = penalty;
        this.maxUses = maxUses;
    }

    @Override
    public Points calculatePoints(List<Annotation> annotations) {
        int multiplier = this.maxUses == null ? annotations.size() : Math.min(annotations.size(), this.maxUses);
        double penalty = multiplier * -this.penalty;
        return new Points(penalty, this.maxUses != null && annotations.size() > this.maxUses);
    }
}
