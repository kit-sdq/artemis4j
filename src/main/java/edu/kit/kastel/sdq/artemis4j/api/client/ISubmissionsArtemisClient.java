/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Submission;

import java.util.ArrayList;
import java.util.List;

/**
 * REST-Client to execute calls concerning submissions.
 */
public interface ISubmissionsArtemisClient {

	/**
	 * Get all submissions for the given exercise.
	 *
	 * @param exercise exercise to load submissions.
	 * @return submissions for the given exercise.
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */
	default List<Submission> getSubmissions(Exercise exercise) throws ArtemisClientException {
		List<Submission> submissions = new ArrayList<>(this.getSubmissions(exercise, 0));

		if (exercise.hasSecondCorrectionRound()) {
			submissions.addAll(this.getSubmissions(exercise, 1));
		}

		return submissions;
	}

	/**
	 * Get all submissions for the given exercise and correction round.
	 *
	 * @return submissions for the given exercise and correction round.
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */
	default List<Submission> getSubmissions(Exercise exercise, int correctionRound) throws ArtemisClientException {
		return this.getSubmissions(exercise, correctionRound, false);
	}

	/**
	 * Get all submissions for the given exercise and correction round.
	 *
	 * @return submissions for the given exercise and correction round.
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */
	List<Submission> getSubmissions(Exercise exercise, int correctionRound, boolean filterAssessedByTutor) throws ArtemisClientException;

	/**
	 * Get submission with submissionId for the given exercise.
	 *
	 * @param exercise     exercise to load submission.
	 * @param submissionId of submission to be returned
	 * @return submission with submissionId.
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */
	Submission getSubmissionById(Exercise exercise, int submissionId) throws ArtemisClientException;

}
