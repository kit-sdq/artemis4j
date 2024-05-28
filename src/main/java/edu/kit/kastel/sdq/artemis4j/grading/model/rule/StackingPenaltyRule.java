/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.rule;

import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

public class StackingPenaltyRule extends PenaltyRule {

	private static final String DISPLAY_NAME = "Stacking Penalty";
	public static final String SHORT_NAME = "stackingPenalty";

	private final double penalty;

	private Integer maxUses = null; // null => no limit

	public StackingPenaltyRule(JsonNode penaltyRuleNode) {
		this.penalty = penaltyRuleNode.get("score").asDouble();

		if (penaltyRuleNode.hasNonNull("maxUses")) {
			maxUses = penaltyRuleNode.get("maxUses").asInt();
		}
	}

	@Override
	public double calculate(List<IAnnotation> annotations) {
		int multiplier = maxUses == null ? annotations.size() : Math.min(annotations.size(), maxUses);
		return (multiplier * -this.penalty);
	}

	@Override
	public boolean limitReached(List<IAnnotation> annotations) {
		return maxUses != null && annotations.size() >= maxUses;
	}

	@Override
	public String getDisplayName() {
		return DISPLAY_NAME;
	}

	@Override
	public String getShortName() {
		return SHORT_NAME;
	}

	@Override
	public String getTooltip(List<IAnnotation> annotations) {
		double penaltyValue = this.calculate(annotations);
		String tooltip = penaltyValue + " points [" + annotations.size() + " annotations made";
		tooltip += maxUses != null ? " - capped to " + maxUses + " annotations" : "";
		tooltip += "]";
		return tooltip;
	}

	@Override
	public String toString() {
		String string = "StackingPenaltyRule [score=" + this.penalty + " per annotation";
		string += maxUses != null ? " capped to " + maxUses + " annotations" : "";
		string += "]";
		return string;
	}

	@Override
	public boolean isCustomPenalty() {
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(maxUses, penalty);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		StackingPenaltyRule other = (StackingPenaltyRule) obj;
		return Objects.equals(maxUses, other.maxUses) && penalty == other.penalty;
	}

}
