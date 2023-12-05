/* Licensed under EPL-2.0 2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Feedback;

public interface IFeedbackClient {
	/**
	 * Get the long feedback for a feedback.
	 */
	String getLongFeedback(int resultId, Feedback feedback) throws ArtemisClientException;
}
