/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Exercise;
import edu.kit.kastel.sdq.artemis4j.api.artemis.ExerciseStats;
import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.*;

import java.util.Optional;

/**
 * REST-Client to execute calls concerning assessment.
 */
public interface IAssessmentArtemisClient {

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
	 * Acquires a lock in the process.
	 *
	 * @param correctionRound relevant for exams! may be 0 or 1
	 * @return
	 *         <li>the data gotten back. Needed for submitting correctly.
	 *         <li><b>null</b> if there is no submission left to correct
	 * @throws ArtemisClientException if some errors occur while parsing the result
	 *                                or if authentication fails.
	 */
	Optional<LockResult> startNextAssessment(Exercise exercise, int correctionRound) throws ArtemisClientException;

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
	 * Get statistics for exercise.
	 */
	ExerciseStats getStats(Exercise exercise) throws ArtemisClientException;

	/**
	 * Get the long feedback for a feedback.
	 */
	LongFeedbackText getLongFeedback(int resultId, Feedback feedback) throws ArtemisClientException;
}
