package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

import java.util.Map;
import java.util.Objects;

public final class MistakeType {
    private final String id;
    private final PenaltyRule rule;
    private final RatingGroup ratingGroup;
    private final FormatString message;
    private final FormatString buttonTexts;

    MistakeType(MistakeTypeDTO dto, RatingGroup ratingGroup) {
        this.id = dto.shortName();
        this.rule = dto.penaltyRule();
        this.ratingGroup = ratingGroup;
        this.message = new FormatString(dto.message(), dto.additionalMessages());
        this.buttonTexts = new FormatString(dto.button(), dto.additionalButtonTexts());

        // Add ourselves to the rating group
        ratingGroup.addMistakeType(this);
    }

    public String getId() {
        return id;
    }

    public PenaltyRule getRule() {
        return rule;
    }

    public RatingGroup getRatingGroup() {
        return ratingGroup;
    }

    public TranslatableString getMessage() {
        return message.format();
    }

    public TranslatableString getButtonText() {
        return buttonTexts.format();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MistakeType that = (MistakeType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /* package-private */ record MistakeTypeDTO(String shortName, String message, String button,
                                                   PenaltyRule penaltyRule, String appliesTo,
                                                   String enabledForExercises, String enabledPenaltyForExercises,
                                                   Map<String, String> additionalButtonTexts,
                                                   Map<String, String> additionalMessages) {
    }
}
