/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

public final class RatingGroup {
    private final String id;
    private final FormatString displayName;
    private final double minPenalty;
    private final double maxPenalty;
    private final List<MistakeType> mistakeTypes;

    RatingGroup(RatingGroupDTO dto) {
        double negativeLimit = dto.negativeLimit() != null ? dto.negativeLimit() : Double.NEGATIVE_INFINITY;
        double positiveLimit = dto.positiveLimit() != null ? dto.positiveLimit() : Double.POSITIVE_INFINITY;

        if (negativeLimit > positiveLimit) {
            throw new IllegalArgumentException("Invalid penalty range for rating group: %fP -- %fP"
                    .formatted(dto.negativeLimit(), dto.positiveLimit()));
        }

        this.id = dto.shortName();
        this.displayName = new FormatString(dto.displayName(), dto.additionalDisplayNames());
        this.minPenalty = negativeLimit;
        this.maxPenalty = positiveLimit;
        this.mistakeTypes = new ArrayList<>();
    }

    public List<MistakeType> getMistakeTypes() {
        return Collections.unmodifiableList(this.mistakeTypes);
    }

    public String getId() {
        return id;
    }

    public TranslatableString getDisplayName() {
        return displayName.format();
    }

    public double getMinPenalty() {
        return minPenalty;
    }

    public double getMaxPenalty() {
        return maxPenalty;
    }

    public boolean isScoringGroup() {
        return minPenalty != 0 || maxPenalty != 0;
    }

    /**
     * Only used during deserialization to create the circular references between
     * RatingGroup and MistakeType
     */
    void addMistakeType(MistakeType mistakeType) {
        this.mistakeTypes.add(mistakeType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingGroup that = (RatingGroup) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "RatingGroup[" + "id=" + id + ", " + "displayName=" + displayName.getDefaultTranslationPattern() + ", "
                + "minPenalty=" + minPenalty + ", " + "getMaxPenalty=" + maxPenalty + ", " + "mistakeTypes="
                + mistakeTypes + ']';
    }

    record RatingGroupDTO(
            String shortName,
            String displayName,
            Double positiveLimit,
            Double negativeLimit,
            Map<String, String> additionalDisplayNames) {}
}
