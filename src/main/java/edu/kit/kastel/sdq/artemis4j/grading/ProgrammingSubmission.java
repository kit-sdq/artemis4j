/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AssessmentType;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;

/**
 * A student's programming submission. A submission essentially consists of the
 * URL of a student's Git repository, along with a commit hash. We do not model
 * the separate 'participation' entity in Artemis.
 */
public class ProgrammingSubmission extends ArtemisConnectionHolder {
    private final ProgrammingSubmissionDTO dto;

    private final int correctionRound;
    private final User student;
    private final ProgrammingExercise exercise;

    public ProgrammingSubmission(ProgrammingSubmissionDTO dto, ProgrammingExercise exercise, int correctionRound) {
        super(exercise);

        this.dto = dto;
        this.exercise = exercise;

        // The student is only present for instructors
        var student = dto.participation().student();
        if (student != null) {
            this.student = new User(student);
        } else {
            this.student = null;
        }

        this.correctionRound = correctionRound;
    }

    public long getId() {
        return this.dto.id();
    }

    public long getParticipationId() {
        return this.dto.participation().id();
    }

    public String getParticipantIdentifier() {
        return this.dto.participation().participantIdentifier();
    }

    public String getRepositoryUrl() {
        return this.dto.participation().userIndependentRepositoryUri();
    }

    public String getCommitHash() {
        return this.dto.commitHash();
    }

    public boolean hasBuildFailed() {
        return this.dto.buildFailed();
    }

    /**
     * The student can only be retrieved by instructors.
     */
    public Optional<User> getStudent() {
        return Optional.ofNullable(this.student);
    }

    public ProgrammingExercise getExercise() {
        return exercise;
    }

    public int getCorrectionRound() {
        return this.correctionRound;
    }

    /**
     * Clones the submission, including the test repository, into the given path,
     * and checks out the submitted commit.
     *
     * @param target        The path to clone into
     * @param tokenOverride (optional) The git password to use for cloning
     * @return The path to the actual submission within the target location
     */
    public ClonedProgrammingSubmission cloneInto(Path target, String tokenOverride) throws ArtemisClientException {
        return ClonedProgrammingSubmission.cloneSubmission(this, target, tokenOverride);
    }

    /**
     * Tries to lock this submission. Locking is reentrant.
     *
     * @return An empty optional if a *different* user has already locked the
     *         submission, otherwise the assessment
     * @throws AnnotationMappingException    If the annotations that were already
     *                                       present could not be mapped given the
     *                                       gradingConfig
     * @throws ArtemisNetworkException       Generic network failure
     * @throws MoreRecentSubmissionException If the requested submission is not the
     *                                       most recent submission of the
     *                                       corresponding student (i.e.
     *                                       participation)
     */
    public Optional<Assessment> tryLock(GradingConfig gradingConfig)
            throws AnnotationMappingException, ArtemisNetworkException, MoreRecentSubmissionException {
        return this.exercise.tryLockSubmission(this.getId(), this.getCorrectionRound(), gradingConfig);
    }

    public boolean isSubmitted() {
        var result = this.getRelevantResult();
        if (result.isEmpty() || result.get().completionDate() == null) {
            return false;
        }

        var assessmentType = result.get().assessmentType();
        return assessmentType == AssessmentType.MANUAL || assessmentType == AssessmentType.SEMI_AUTOMATIC;
    }

    /**
     * Opens the assessment for this submission.
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
    public Optional<Assessment> openAssessment(GradingConfig config)
            throws AnnotationMappingException, ArtemisNetworkException {
        ResultDTO resultDTO = this.getRelevantResult().orElse(null);

        if (resultDTO != null) {
            return Optional.of(new Assessment(resultDTO, config, this, this.correctionRound));
        }

        return Optional.empty();
    }

    public boolean isBuildFailed() {
        return this.dto.buildFailed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProgrammingSubmission that = (ProgrammingSubmission) o;
        return this.getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    private Optional<ResultDTO> getRelevantResult() {
        if (this.dto.results().isEmpty()) {
            return Optional.empty();
        } else if (this.dto.results().size() == 1) {
            // We only have one result, so the submission has
            // probably been created for a specific correction round,
            // or we only have one correction round
            return Optional.of(this.dto.results().get(0));
        } else {
            // More than one result, so probably multiple correction rounds
            return Optional.of(this.dto.nonAutomaticResults().get(this.correctionRound));
        }
    }
}
