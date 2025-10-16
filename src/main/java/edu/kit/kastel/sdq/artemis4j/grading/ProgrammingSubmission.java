/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import org.jspecify.annotations.Nullable;

/**
 * A student's programming submission. A submission essentially consists of the
 * URL of a student's Git repository, along with a commit hash. We do not model
 * the separate 'participation' entity in Artemis.
 */
public class ProgrammingSubmission extends ArtemisConnectionHolder {
    private final ProgrammingSubmissionDTO dto;

    private final @Nullable User student;
    private final ProgrammingExercise exercise;

    public ProgrammingSubmission(ProgrammingSubmissionDTO dto, ProgrammingExercise exercise) {
        super(exercise);

        this.dto = dto;
        this.exercise = exercise;

        // The student is only present for instructors
        var studentDto = dto.participation().student();
        if (studentDto != null) {
            this.student = new User(studentDto);
        } else {
            this.student = null;
        }
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

    public ZonedDateTime getSubmissionDate() {
        return this.dto.submissionDate();
    }

    public Optional<ResultDTO> getLatestResult() {
        if (this.dto.results() != null && !this.dto.results().isEmpty()) {
            return Optional.of(this.dto.results().get(this.dto.results().size() - 1));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Clones the submission, including the test repository, into the given path,
     * and checks out the submitted commit. This method uses the user's VCS access token, potentially creating a new one.
     *
     * @param target        The path to clone into
     * @param tokenOverride (optional) The git password to use for cloning. If not set, the PAT is used (and created if necessary)
     * @return The path to the actual submission within the target location
     */
    public ClonedProgrammingSubmission cloneViaVCSTokenInto(Path target, String tokenOverride)
            throws ArtemisClientException {
        return ClonedProgrammingSubmission.cloneSubmissionViaVCSToken(this, target, tokenOverride);
    }

    /**
     * Clones the submission, including the test repository, into the given path,
     * and checks out the submitted commit. This method uses the user's SSH key, and may interactively prompt the user for their SSH key passphrase.
     *
     * @param target        The path to clone into
     * @return The path to the actual submission within the target location
     */
    public ClonedProgrammingSubmission cloneViaSSHInto(Path target) throws ArtemisClientException {
        return ClonedProgrammingSubmission.cloneSubmissionViaSSH(this, target);
    }

    /**
     * Prefer the methods on PackedAssessment!
     * <p>
     * Tries to lock this submission for a given correction round. Locking is reentrant.
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
    public Optional<Assessment> tryLock(GradingConfig gradingConfig, CorrectionRound correctionRound)
            throws AnnotationMappingException, ArtemisNetworkException, MoreRecentSubmissionException {
        return this.exercise.tryLockSubmission(this.getId(), correctionRound, gradingConfig);
    }

    public boolean isBuildFailed() {
        return this.dto.buildFailed();
    }

    /**
     * Be VERY careful with the dto's results, since their number may differ depending on the source of the dto.
     * @return the corresponding dto
     */
    public ProgrammingSubmissionDTO getDTO() {
        return this.dto;
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
}
