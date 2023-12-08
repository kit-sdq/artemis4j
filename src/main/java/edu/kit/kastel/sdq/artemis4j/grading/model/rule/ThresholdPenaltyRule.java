/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.rule;

import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Objects;

/**
 * A Penalty which returns only one Annotation.
 */
public class ThresholdPenaltyRule extends PenaltyRule {

	private static final String DISPLAY_NAME = "Threshold Penalty";
	public static final String SHORT_NAME = "thresholdPenalty";

	private final int threshold;
	private final double penalty;

	public ThresholdPenaltyRule(JsonNode penaltyRuleNode) {
		this.threshold = penaltyRuleNode.get("threshold").asInt();
		this.penalty = penaltyRuleNode.get("penalty").asDouble();
	}

	public ThresholdPenaltyRule(int threshold, double penalty) {
		this.threshold = threshold;
		this.penalty = penalty;
	}

	@Override
	public double calculate(List<IAnnotation> annotations) {
		return annotations.size() >= this.threshold ? -this.penalty : 0.D;
	}

	@Override
	public boolean limitReached(List<IAnnotation> annotations) {
		return annotations.size() >= this.threshold;
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
		if (penalty == 0) {
			return annotations.size() + " annotations. No deduction will be made.";
		}
		double penaltyValue = this.calculate(annotations);
		return penaltyValue + " points [" + annotations.size() + " of at least " + this.threshold + " annotations made]";
	}

	@Override
	public String toString() {
		return "ThresholdPenaltyRule [threshold=" + this.threshold + ", penalty=" + this.penalty + "]";
	}

	@Override
	public boolean isCustomPenalty() {
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(penalty, threshold);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ThresholdPenaltyRule other = (ThresholdPenaltyRule) obj;
		return Double.doubleToLongBits(penalty) == Double.doubleToLongBits(other.penalty) && threshold == other.threshold;
	}

}
