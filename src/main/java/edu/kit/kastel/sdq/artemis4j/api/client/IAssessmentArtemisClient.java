/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.ExerciseStats;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.*;

import java.util.List;
import java.util.Optional;

/**
 * REST-Client to execute calls concerning assessment.
 */
public interface IAssessmentArtemisClient {

	LockResult startAssessment(int submissionId, int correctionRound) throws ArtemisClientException;

	/**
	 * Starts an assessment for the given submission. Acquires a lock in the
	 * process.
	 *
	 * @return the data gotten back, which is needed for submitting the assessment
	 *         result correctly
	 *         ({@link #saveAssessment(int, boolean, AssessmentResult)}
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */

	LockResult startAssessment(Submission submission) throws ArtemisClientException;

	/**
	 * Starts an assessment for any available submission (determined by artemis).
	 * Acquires a lock in the process.<b>Important: The Lock Result has not loaded
	 * the internal feedbacks correctly!</b>
	 *
	 * @param correctionRound relevant for exams! may be 0 or 1
	 * @return the submissionId that has been locked.
	 * @throws ArtemisClientException if some errors occur while parsing the result
	 *                                or if authentication fails.
	 */
	Optional<Integer> startNextAssessment(Exercise exercise, int correctionRound) throws ArtemisClientException;

	/**
	 * Submit the assessment to Artemis. Must have been started by
	 * {@link #startAssessment(Submission)} or
	 * {@link #startNextAssessment(Exercise, int)} before!
	 *
	 * @param participationId The participationId can be gotten from the
	 *                        {@link LockResult}, via
	 *                        {@link #startAssessment(Submission)} or
	 *                        {@link #startNextAssessment(Exercise, int)}!
	 * @param submit          determine whether the assessment should be submitted
	 *                        or just saved.
	 * @param assessment      the assessment
	 */
	void saveAssessment(int participationId, boolean submit, AssessmentResult assessment) throws ArtemisClientException;

	/**
	 * Get statistics of an exercise.
	 */
	ExerciseStats getStats(Exercise exercise) throws ArtemisClientException;

	/**
	 * Get the feedbacks of a result.
	 */
	List<Feedback> getFeedbacks(Submission submission, Result result) throws ArtemisClientException;

	/**
	 * Get the long feedback for a feedback.
	 */
	String getLongFeedback(int resultId, Feedback feedback) throws ArtemisClientException;
}
