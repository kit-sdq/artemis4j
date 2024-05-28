package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "shortName")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StackingPenaltyRule.class, name = "stackingPenalty"),
        @JsonSubTypes.Type(value = ThresholdPenaltyRule.class, name = "thresholdPenalty"),
        @JsonSubTypes.Type(value = CustomPenaltyRule.class, name = "customPenalty")
})
public sealed interface PenaltyRule permits CustomPenaltyRule, StackingPenaltyRule, ThresholdPenaltyRule {
    Points calculatePoints(List<Annotation> annotations);

    default boolean isCustomPenalty() {
        return false;
    }
}