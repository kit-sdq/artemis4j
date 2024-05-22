package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

import java.text.MessageFormat;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "shortName")
@JsonSubTypes({
        @JsonSubTypes.Type(value = StackingPenaltyRule.class, name = "stackingPenalty"),
        @JsonSubTypes.Type(value = ThresholdPenaltyRule.class, name = "thresholdPenalty"),
        @JsonSubTypes.Type(value = CustomPenaltyRule.class, name = "customPenalty")
})
public sealed interface PenaltyRule permits CustomPenaltyRule, StackingPenaltyRule, ThresholdPenaltyRule {
    FormatString ARTEMIS_CUSTOM_MESSAGE = new FormatString(new MessageFormat("{0}\nExplanation: {1}"));

    Points calculatePenalty(List<Annotation> annotations);

    default TranslatableString formatMessageForArtemis(Annotation annotation) {
        var mistakeMessage = annotation.getMistakeType().getMessage();
        if (annotation.getCustomMessage().isPresent()) {
            return ARTEMIS_CUSTOM_MESSAGE.format(mistakeMessage, annotation.getCustomMessage().get());
        } else {
            return mistakeMessage;
        }
    }
}
