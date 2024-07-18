/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.kit.kastel.sdq.artemis4j.grading.Annotation;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "shortName")
@JsonSubTypes({ @JsonSubTypes.Type(value = StackingPenaltyRule.class, name = "stackingPenalty"),
        @JsonSubTypes.Type(value = ThresholdPenaltyRule.class, name = "thresholdPenalty"),
        @JsonSubTypes.Type(value = CustomPenaltyRule.class, name = "customPenalty") })
public sealed interface PenaltyRule permits CustomPenaltyRule, StackingPenaltyRule, ThresholdPenaltyRule {
    Points calculatePoints(List<Annotation> annotations);
}
