/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.grading;

import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;

import java.util.List;

/**
 * Represents one type of mistakes from a rating group.
 */
public interface IMistakeType {

	/**
	 * Calculate penalty using only the given annotations.
	 *
	 * @return a <i>positive or negative</i> value denoting the penalty or points.
	 */
	double calculate(List<IAnnotation> annotations);

	/**
	 * @param annotations the annotations on which the calculation is based.
	 * @return whether the maximum penalty or points limit of annotations for this
	 *         mistake type has been reached.
	 */

	boolean limitReached(List<IAnnotation> annotations);

	/**
	 * @return what should be used as unique id.
	 */
	String getIdentifier();

	/**
	 * @param languageKey the key of the language that is selected. (e.g., "de").
	 *                    Can be null to use the default language.
	 * @return a more elaborate explanation of what the mistake is.
	 */
	String getMessage(String languageKey);

	/**
	 * @param languageKey the key of the language that is selected. (e.g., "de").
	 *                    Can be null to use the default language.
	 * @return what should be shown on the button.
	 */
	String getButtonText(String languageKey);

	/**
	 * @return the {@link IRatingGroup} this {@link IMistakeType} belongs to, which
	 *         may introduce a {@link IRatingGroup#getRange()}!
	 */
	IRatingGroup getRatingGroup();

	/**
	 * @param languageKey the key of the language that is selected. (e.g., "de").
	 *                    Can be null to use the default language.
	 * @return tooltip for hovering over the button. Shows rating status information
	 *         based on the given annotation.
	 */
	String getTooltip(String languageKey, List<IAnnotation> annotations);

	/**
	 * Indicates whether this is a custom penalty.
	 *
	 * @return indicator for custom penalties
	 */
	boolean isCustomPenalty();

	void initialize(Exercise exercise);

	boolean isEnabledMistakeType();

	boolean isEnabledPenalty();
}
