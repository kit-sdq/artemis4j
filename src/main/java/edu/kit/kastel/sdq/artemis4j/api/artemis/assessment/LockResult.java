/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.assessment;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.client.IAssessmentArtemisClient;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * This is gotten from acquiring a lock (no matter if the lock is already held
 * by the caller or not). It is used to calculate the assessment result.
 */
public class LockResult {
	@Serial
	private static final long serialVersionUID = -3787474578751131899L;

	private final int submissionId;
	private final int participationId;

	private int resultId;
	private final List<Feedback> latestFeedback;

	@JsonCreator
	public LockResult( //
			@JsonProperty("id") int submissionId, //
			@JsonProperty("results") List<LockCallAssessmentResult> previousAssessmentResults, //
			@JsonProperty("participation") Participation participation) {

		this.submissionId = submissionId;
		this.participationId = participation.getParticipationId();

		this.latestFeedback = new ArrayList<>();
		LockCallAssessmentResult latestResult = previousAssessmentResults.isEmpty() //
				? null //
				: previousAssessmentResults.get(previousAssessmentResults.size() - 1);

		if (latestResult != null) {
			resultId = latestResult.getId();
			latestResult.getFeedbacks().stream().filter(Objects::nonNull).forEach(this.latestFeedback::add);
		}
	}

	public void init(IAssessmentArtemisClient assessmentClient) throws ArtemisClientException {
		for (Feedback feedback : this.latestFeedback) {
			boolean hasLongFeedbackText = feedback.hasLongFeedbackText();

			// Needed for storing the information later on.
			feedback.resetLongFeedbackProperty();

			if (!hasLongFeedbackText) {
				continue;
			}

			LongFeedbackText actualFeedback = assessmentClient.getLongFeedback(resultId, feedback);
			feedback.setDetailTextComplete(actualFeedback.getText());
		}

	}

	/**
	 * @return the participationId this submissionId belongs to (one participation
	 *         has one or many submissions).
	 */
	public int getParticipationId() {
		return this.participationId;
	}

	/**
	 * @return all {@link Feedback Feedbacks} that are saved in Artemis. This is
	 *         used to calculate the assessment result which is sent back to
	 *         Artemis.
	 */
	public List<Feedback> getLatestFeedback() {
		return this.latestFeedback;
	}

	/**
	 * @return the submissionId this result belongs to (one participation has one or
	 *         many submissions).
	 */
	public int getSubmissionId() {
		return this.submissionId;
	}

}
