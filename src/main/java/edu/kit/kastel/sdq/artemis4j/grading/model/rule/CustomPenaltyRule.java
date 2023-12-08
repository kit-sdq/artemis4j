/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.rule;

import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class CustomPenaltyRule extends PenaltyRule {

	private static final String DISPLAY_NAME = "Custom Penalty";
	public static final String SHORT_NAME = "customPenalty";

	public CustomPenaltyRule(JsonNode penaltyRuleNode) {
		// No need for custom configurations.
	}

	@Override
	public double calculate(List<IAnnotation> annotations) {
		if (annotations != null) {
			return annotations.stream().mapToDouble(annotation -> annotation.getCustomPenalty().orElse(0.D)).sum();
		}
		return 0.D;
	}

	@Override
	public boolean limitReached(List<IAnnotation> annotations) {
		return false;
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
		return this.calculate(annotations) + " points [" + annotations.size() + " annotations made]";
	}

	@Override
	public boolean isCustomPenalty() {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		return this.getClass() == obj.getClass();
	}

	@Override
	public int hashCode() {
		return this.getClass().hashCode();
	}
}
