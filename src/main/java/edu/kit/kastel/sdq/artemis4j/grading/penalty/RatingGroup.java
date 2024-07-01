package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public final class RatingGroup {
    private final String id;
    private final FormatString displayName;
    private final double minPenalty;
    private final double maxPenalty;
    private final List<MistakeType> mistakeTypes;

    /* package-private */ RatingGroup(RatingGroupDTO dto) {
        if (dto.negativeLimit() > dto.positiveLimit()) {
            throw new IllegalArgumentException("Invalid penalty range for rating group: %fP -- %fP".formatted(dto.negativeLimit(), dto.positiveLimit()));
        }

        this.id = dto.shortName();
        this.displayName = new FormatString(dto.displayName(), dto.additionalDisplayNames());
        this.minPenalty = dto.negativeLimit();
        this.maxPenalty = dto.positiveLimit();
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
     * Only used during deserialization to create the circular references between RatingGroup and MistakeType
     *
     * @param mistakeType
     */
    /* package-private */ void addMistakeType(MistakeType mistakeType) {
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
        return "RatingGroup[" +
                "id=" + id + ", " +
                "displayName=" + displayName + ", " +
                "minPenalty=" + minPenalty + ", " +
                "getMaxPenalty=" + maxPenalty + ", " +
                "mistakeTypes=" + mistakeTypes + ']';
    }

    /* package-private */ record RatingGroupDTO(String shortName, String displayName, Double positiveLimit,
                                                Double negativeLimit, Map<String, String> additionalDisplayNames) {
    }
}
