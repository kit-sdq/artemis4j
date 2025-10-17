/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import org.jspecify.annotations.Nullable;

public final class MistakeType {
    private final String id;
    private final PenaltyRule rule;
    private final RatingGroup ratingGroup;
    private final FormatString message;
    private final FormatString buttonTexts;
    private final MistakeReportingState reporting;
    private final List<String> autograderProblemTypes;
    private final Highlight highlight;

    static void createAndAddToGroup(MistakeTypeDTO dto, boolean shouldScore, RatingGroup ratingGroup) {
        var mistakeType = new MistakeType(dto, shouldScore, ratingGroup);
        ratingGroup.addMistakeType(mistakeType);
    }

    private MistakeType(MistakeTypeDTO dto, boolean shouldScore, RatingGroup ratingGroup) {
        this.id = dto.shortName();
        this.rule = dto.penaltyRule();
        this.ratingGroup = ratingGroup;
        this.message = new FormatString(dto.message(), dto.additionalMessages());
        this.buttonTexts = new FormatString(dto.button(), dto.additionalButtonTexts());

        if (dto.autograderProblemTypes() != null) {
            this.autograderProblemTypes = dto.autograderProblemTypes();
        } else {
            this.autograderProblemTypes = List.of();
        }

        if (shouldScore) {
            this.reporting = MistakeReportingState.REPORT_AND_SCORE;
        } else {
            this.reporting = MistakeReportingState.REPORT;
        }

        this.highlight = dto.highlight() == null ? Highlight.DEFAULT : dto.highlight();
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

    public boolean isAssociatedWith(RatingGroup group) {
        return this.ratingGroup.isAssociatedWith(group);
    }

    /**
     * Returns the display name of the representative group for this mistake types group.
     * For a group that is not a subgroup, this will return the {@link RatingGroup#getDisplayName()} of the group.
     * For a subgroup, this will return the {@link RatingGroup#getDisplayName()} of the main <b>parent</b> group.
     *
     * @return the display name of the representative group
     */
    public TranslatableString getRepresentativeGroupDisplayName() {
        RatingGroup group = this.ratingGroup;

        while (group.getParent() != null) {
            group = group.getParent();
        }

        return group.getDisplayName();
    }

    public TranslatableString getMessage() {
        return message.format();
    }

    public TranslatableString getButtonText() {
        return buttonTexts.format();
    }

    public boolean shouldScore() {
        return this.reporting.shouldScore();
    }

    public boolean isCustomAnnotation() {
        return this.rule instanceof CustomPenaltyRule;
    }

    public MistakeReportingState getReporting() {
        return reporting;
    }

    public List<String> getAutograderProblemTypes() {
        return Collections.unmodifiableList(autograderProblemTypes);
    }

    /**
     * Returns how the mistake type should be highlighted.
     *
     * @return the highlight for the mistake
     */
    public Highlight getHighlight() {
        return this.highlight;
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

    @Override
    public String toString() {
        return "MistakeType{" + "id='"
                + id + '\'' + ", rule="
                + rule + ", ratingGroup="
                + ratingGroup + ", message="
                + message.getDefaultTranslationPattern() + ", buttonTexts="
                + buttonTexts.getDefaultTranslationPattern() + ", reporting="
                + reporting + ", autograderProblemTypes="
                + autograderProblemTypes + ", highlight="
                + highlight + '}';
    }

    public enum Highlight {
        NONE,
        DEFAULT;

        @JsonValue
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    record MistakeTypeDTO(
            String shortName,
            String message,
            String button,
            PenaltyRule penaltyRule,
            String appliesTo,
            String enabledForExercises,
            String enabledPenaltyForExercises,
            Map<String, String> additionalButtonTexts,
            Map<String, String> additionalMessages,
            @Nullable List<String> autograderProblemTypes,
            Highlight highlight) {}
}
