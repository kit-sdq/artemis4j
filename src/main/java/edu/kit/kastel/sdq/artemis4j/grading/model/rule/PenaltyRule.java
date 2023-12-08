/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.rule;

import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;
import edu.kit.kastel.sdq.artemis4j.grading.config.PenaltyRuleDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

/**
 * This class is used by an IMistakeType-Instance to calculate penalties.
 */
@JsonDeserialize(using = PenaltyRuleDeserializer.class)
public abstract class PenaltyRule {

	/**
	 * Calculate the penalty these Annotations add to the result.
	 *
	 * @param annotations the annotations on which the calculation is based.
	 * @return a reducing penalty
	 */
	public abstract double calculate(List<IAnnotation> annotations);

	/**
	 * @return true if the maximum limit is reached, false otherwise.
	 */
	public abstract boolean limitReached(List<IAnnotation> annotations);

	public abstract String getDisplayName();

	public abstract String getShortName();

	/**
	 * @return a tooltip String (e.g. for thresholdPenalty the score and how many
	 *         annotations were made.)
	 */
	public abstract String getTooltip(List<IAnnotation> annotations);

	public abstract boolean isCustomPenalty();
}
