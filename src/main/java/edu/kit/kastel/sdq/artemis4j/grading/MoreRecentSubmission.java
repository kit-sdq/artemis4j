/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

/**
 * The requested submission is not the most recent one, but Artemis only
 * supports e.g. locking the most recent submission for a given participation.
 */
public class MoreRecentSubmission extends ArtemisClientException {
	public MoreRecentSubmission(long oldSubmissionId, long newSubmissionId, long participationId) {
		super("There is a more recent submission (%d) than the requested submission %d for participation %d".formatted(newSubmissionId, oldSubmissionId,
				participationId));
	}
}
