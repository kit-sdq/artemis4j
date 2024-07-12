/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import de.firemage.autograder.core.ProblemType;
import edu.kit.kastel.sdq.artemis4j.i18n.FormatString;
import edu.kit.kastel.sdq.artemis4j.i18n.TranslatableString;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MistakeType {
	private final String id;
	private final PenaltyRule rule;
	private final RatingGroup ratingGroup;
	private final FormatString message;
	private final FormatString buttonTexts;
	private final MistakeReportingState reporting;
	private final List<ProblemType> autograderProblemTypes;

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

	public boolean shouldScore() {
		return this.reporting.shouldScore();
	}

	public boolean isCustomAnnotation() {
		return this.rule instanceof CustomPenaltyRule;
	}

	public List<ProblemType> getAutograderProblemTypes() {
		return Collections.unmodifiableList(autograderProblemTypes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		MistakeType that = (MistakeType) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	/* package-private */ record MistakeTypeDTO(String shortName, String message, String button, PenaltyRule penaltyRule, String appliesTo,
			String enabledForExercises, String enabledPenaltyForExercises, Map<String, String> additionalButtonTexts, Map<String, String> additionalMessages,
			List<ProblemType> autograderProblemTypes) {
	}
}
