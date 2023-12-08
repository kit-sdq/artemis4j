/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;
import edu.kit.kastel.sdq.artemis4j.api.grading.IMistakeType;
import edu.kit.kastel.sdq.artemis4j.api.grading.IRatingGroup;
import edu.kit.kastel.sdq.artemis4j.grading.model.rule.PenaltyRule;
import edu.kit.kastel.sdq.artemis4j.grading.model.rule.ThresholdPenaltyRule;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MistakeType implements IMistakeType {
	@JsonProperty("shortName")
	private String identifier;

	@JsonProperty("button")
	private String buttonText;
	/**
	 * Map of additional languages for the button text. Format of map: {@code {"en"
	 * -> "Button Text in English"}}
	 */
	@JsonProperty("additionalButtonTexts")
	private Map<String, String> additionalButtonTexts;

	@JsonProperty("message")
	private String message;
	/**
	 * Map of additional languages for the message. Format of map: {@code {"en" ->
	 * "Message in English"}}
	 */
	@JsonProperty("additionalMessages")
	private Map<String, String> additionalMessages;

	@JsonProperty("appliesTo")
	private String appliesTo;

	private RatingGroup ratingGroup;
	@JsonProperty("penaltyRule")
	private PenaltyRule penaltyRule;

	@JsonProperty("enabledForExercises")
	private String enabledForExercises;
	@JsonProperty("enabledPenaltyForExercises")
	private String enabledPenaltyForExercises;

	private transient Exercise currentExercise = null;

	@Override
	public double calculate(List<IAnnotation> annotations) {
		assert annotations.stream().allMatch(a -> this.equals(a.getMistakeType()));
		return this.getPenaltyRule().calculate(annotations);
	}

	@Override
	public boolean limitReached(List<IAnnotation> annotations) {
		assert annotations.stream().allMatch(a -> this.equals(a.getMistakeType()));
		return this.getPenaltyRule().limitReached(annotations);
	}

	/**
	 * @return to which rating group this applies.
	 */
	public String getAppliesTo() {
		return this.appliesTo;
	}

	@Override
	public String getMessage(String languageKey) {
		if (languageKey == null || additionalMessages == null || !additionalMessages.containsKey(languageKey)) {
			return this.message;
		}
		return additionalMessages.get(languageKey);
	}

	@Override
	public String getButtonText(String languageKey) {
		if (languageKey == null || additionalButtonTexts == null || !additionalButtonTexts.containsKey(languageKey)) {
			return this.buttonText;
		}
		return additionalButtonTexts.get(languageKey);
	}

	@Override
	public void initialize(Exercise exercise) {
		currentExercise = exercise;
	}

	@Override
	public boolean isEnabledMistakeType() {
		if (enabledForExercises == null || currentExercise == null) {
			return true;
		}
		return this.currentExercise.getShortName().matches(this.enabledForExercises);
	}

	@Override
	public boolean isEnabledPenalty() {
		if (enabledPenaltyForExercises == null || currentExercise == null || penaltyRule.isCustomPenalty()) {
			return true;
		}
		return this.currentExercise.getShortName().matches(this.enabledPenaltyForExercises);
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	public PenaltyRule getPenaltyRule() {
		if (isEnabledPenalty()) {
			return this.penaltyRule;
		}
		// Create penalty with zero points deduction
		return new ThresholdPenaltyRule(1, 0);
	}

	@Override
	public IRatingGroup getRatingGroup() {
		return this.ratingGroup;
	}

	@Override
	public String getTooltip(String languageKey, List<IAnnotation> annotations) {
		String penaltyText = getPenaltyRule().getTooltip(annotations);
		return getMessage(languageKey) + "\n" + penaltyText;
	}

	/**
	 * Sets a new rating group if there ain't already one. (Used for
	 * deserialization).
	 *
	 * @param ratingGroup the new rating group
	 */
	public void setRatingGroup(RatingGroup ratingGroup) {
		if (this.ratingGroup == null) {
			this.ratingGroup = ratingGroup;
		}
	}

	@Override
	public String toString() {
		return "MistakeType [identifier=" + this.identifier + ", name=" + this.buttonText + ", message=" + this.message + ", ratingGroup=" + this.ratingGroup
				+ ", penaltyRule=" + this.penaltyRule + "]";
	}

	@Override
	public boolean isCustomPenalty() {
		return this.penaltyRule.isCustomPenalty();
	}

	@Override
	public int hashCode() {
		return Objects.hash(appliesTo, penaltyRule, identifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		MistakeType other = (MistakeType) obj;
		return Objects.equals(appliesTo, other.appliesTo) && Objects.equals(penaltyRule, other.penaltyRule) && Objects.equals(identifier, other.identifier);
	}

}
