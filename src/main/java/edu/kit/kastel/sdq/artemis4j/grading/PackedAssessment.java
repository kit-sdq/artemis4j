package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;

import java.util.Optional;

/**
 * For API users, this is used in place of an assessment whenever we don't want to do
 * the (expensive) deserialization of the assessment.
 * The only semantic difference to an assessment is that this class does *not* imply a lock.
 * <p>
 * Internally, this is just a glorified ResultDTO wrapper because we need a place
 * to store the submission
 * @param result
 * @param submission
 */
public record PackedAssessment(ResultDTO result, CorrectionRound round, ProgrammingSubmission submission) {
    public boolean isSubmitted() {
        return result.completionDate() != null;
    }

    public User getAssessor() {
        return new User(result.assessor());
    }

    /**
     * Locks and opens the assessment for this submission.
     * <p>
     * If the submission has not been assessed by you, this might not be possible.
     *
     * @param config the config for the exercise
     * @return the assessment if there are results for this submission
     * @throws AnnotationMappingException If the annotations that were already
     *                                    present could not be mapped given the
     *                                    gradingConfig
     */
    public Optional<Assessment> open(GradingConfig config) throws MoreRecentSubmissionException, ArtemisNetworkException, AnnotationMappingException {
        return this.submission.getExercise().tryLockSubmission(this.submission.getId(), this.round, config);
    }

    /**
     * Opens the assessment for this submission without locking it.
     * <p>
     * If the submission has not been assessed by you, you might not be able to
     * change the assessment.
     *
     * @param config the config for the exercise
     * @return the assessment if there are results for this submission
     * @throws AnnotationMappingException If the annotations that were already
     *                                    present could not be mapped given the
     *                                    gradingConfig
     */
    public Assessment openWithoutLock(GradingConfig config) throws ArtemisNetworkException, AnnotationMappingException {
        return new Assessment(this.result, config, this.submission, this.round);
    }

    public void cancel() throws ArtemisNetworkException {
        ProgrammingSubmissionDTO.cancelAssessment(this.getConnection().getClient(), this.submission.getId());
    }

    private ArtemisConnection getConnection() {
        return this.submission.getConnection();
    }
}
