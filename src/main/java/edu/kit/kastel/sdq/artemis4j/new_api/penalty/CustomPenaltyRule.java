package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.new_api.Annotation;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

import java.text.MessageFormat;
import java.util.List;

public final class CustomPenaltyRule implements PenaltyRule {
    private static final FormatString ARTEMIS_MESSAGE = new FormatString(new MessageFormat("{0} ({1,number,##.###}P)"));

    @Override
    public Points calculatePenalty(List<Annotation> annotations) {
        return new Points(annotations.stream().mapToDouble(annotation -> annotation.getCustomPenalty().orElseThrow()).sum(), false);
    }

    @Override
    public TranslatableString formatMessageForArtemis(Annotation annotation) {
        return ARTEMIS_MESSAGE.format(annotation.getCustomMessage().orElseThrow(), annotation.getCustomPenalty().orElseThrow());
    }
}
