/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.grading.config;

import edu.kit.kastel.sdq.artemis4j.grading.model.rule.CustomPenaltyRule;
import edu.kit.kastel.sdq.artemis4j.grading.model.rule.PenaltyRule;
import edu.kit.kastel.sdq.artemis4j.grading.model.rule.StackingPenaltyRule;
import edu.kit.kastel.sdq.artemis4j.grading.model.rule.ThresholdPenaltyRule;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Function;

/**
 * A means to construct PenaltyRules without having to edit a switch/case
 * statement: To add a new enum value you merely need to write a lambda
 * constructing your new Subclass of PenaltyRule out of your new custom values
 * which are provided in the penaltyRuleNode.
 */
public enum PenaltyRuleType {
	// Need to add a new enum value with a short Name (that must be used in the
	// config file) and a constructor based on the json node.
	THRESHOLD_PENALTY_RULE_TYPE(ThresholdPenaltyRule.SHORT_NAME, ThresholdPenaltyRule::new), //
	STACKING_PENALTY_RULE_TYPE(StackingPenaltyRule.SHORT_NAME, StackingPenaltyRule::new), //
	CUSTOM_PENALTY_RULE_TYPE(CustomPenaltyRule.SHORT_NAME, CustomPenaltyRule::new);

	public static PenaltyRuleType fromShortName(String shortName) {
		for (PenaltyRuleType penaltyRuleType : PenaltyRuleType.values()) {
			if (penaltyRuleType.getShortName().equalsIgnoreCase(shortName)) {
				return penaltyRuleType;
			}
		}
		return null;
	}

	private final Function<JsonNode, PenaltyRule> constructor;

	private final String shortName;

	PenaltyRuleType(final String shortName, final Function<JsonNode, PenaltyRule> constructor) {
		this.shortName = shortName;
		this.constructor = constructor;
	}

	public PenaltyRule construct(final JsonNode penaltyRuleNode) {
		return this.constructor.apply(penaltyRuleNode);
	}

	public String getShortName() {
		return this.shortName;
	}
}