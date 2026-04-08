/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ParticipationDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;

/**
 * A participation is the student-specific instance of an exercise, and is created
 * when the student starts an exercise.
 * <p>
 * In other words: For a student to be able to submit code for an exercise, they
 * first start a {@link Participation} through {@link ProgrammingExercise#startParticipation()}
 * for that exercise. Then they can submit code for that participation.
 * <p>
 * Each user has one participation. A participation consists of multiple submissions.
 */
public class Participation extends ArtemisConnectionHolder {
    private final ParticipationDTO dto;
    private final Exercise exercise;

    Participation(ParticipationDTO dto, ArtemisConnection connection, Exercise exercise) {
        super(connection);
        this.dto = dto;
        this.exercise = exercise;
    }

    public long getId() {
        return this.dto.id();
    }

    /**
     * This can be used to get the identifier of the participant, which should be equal
     * to the {@link User#getLogin()} of the participant.
     *
     * @return the identifier for the participant
     */
    public String getParticipantIdentifier() {
        return this.dto.participantIdentifier();
    }

    /**
     * Returns the results associated with the current participation instance.
     * <p>
     * With some requests artemis might attach one or more results.
     *
     * @return the list of results or an empty list
     */
    public List<ResultDTO> getResults() {
        return this.dto.results() == null ? List.of() : this.dto.results();
    }

    /**
     * Returns the submissions associated with the current participation instance.
     * <p>
     * With some requests artemis might attach submissions.
     *
     * @return the list of submissions or an empty list
     */
    public List<ProgrammingSubmission> getSubmissions() {
        if (this.dto.submissions() == null) {
            return List.of();
        }

        List<ProgrammingSubmission> submissions = new ArrayList<>();
        for (var submission : this.dto.submissions()) {
            submissions.add(new ProgrammingSubmission(submission, (ProgrammingExercise) this.exercise, this));
        }

        return submissions;
    }

    /**
     * Deletes this participation.
     * <p>
     * Requires at least instructor permissions. For programming exercises, Artemis also
     * removes the build plan and repository, as well as the participation's results and submissions.
     *
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public void delete() throws ArtemisNetworkException {
        ParticipationDTO.delete(this.getConnection().getClient(), this.getId());
    }

    /**
     * Removes the build plan of this programming participation.
     * <p>
     * Requires at least instructor permissions and the ProgrammingExercises feature toggle.
     * The repository itself is kept, so the participation can still be resumed.
     *
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public void cleanupBuildPlan() throws ArtemisNetworkException {
        ParticipationDTO.cleanupBuildPlan(this.getConnection().getClient(), this.getId());
    }

    /**
     * The actual repository URL which can look like this:
     * {@code https://alice@artemis.edu/git/EXERCISE1/exercise.git}
     * <p>
     * Note that it includes the username, if you do not want this included,
     * see {@link Participation#getUserIndependentRepositoryUri()}.
     * <p>
     * The value will not be present, if the repository has not been created/copied
     * yet, or after it has been cleaned up/deleted.
     *
     * @return the repository uri or empty
     */
    public Optional<String> getRepositoryUri() {
        return Optional.ofNullable(this.dto.repositoryUri());
    }

    /**
     * Similar to {@link Participation#getRepositoryUri()}, but with the username
     * stripped, for example it would look like this:
     * {@code https://artemis.edu/git/EXERCISE1/exercise.git}
     * <p>
     * It seems like artemis is using this mainly for display purposes as a preference
     * if it is available. The actual cloning/link building is done with
     * {@link Participation#getRepositoryUri()}.
     * <p>
     * To clone through this link, you have to have extra authentication e.g. an ssh key
     * or a vcs access token see {@link ProgrammingSubmission#cloneViaVCSTokenInto(Path, String)}
     * or {@link ProgrammingSubmission#cloneViaSSHInto(Path)} and enough permissions to
     * clone another users code, for example tutor role.
     * <p>
     * The value can be empty if {@link Participation#getRepositoryUri()} is empty,
     * or it fails to parse/strip the user parts from the url (unlikely).
     *
     * @return the user-independent repository uri or empty
     */
    public Optional<String> getUserIndependentRepositoryUri() {
        return Optional.ofNullable(this.dto.userIndependentRepositoryUri());
    }

    /**
     * Checks if the participation has a result yet.
     *
     * @return true if there is a result, false if not.
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public boolean hasResult() throws ArtemisNetworkException {
        if (!this.dto.results().isEmpty()) {
            return true;
        }

        return ParticipationDTO.hasResult(this.getConnection().getClient(), this.getId());
    }

    /**
     * Gets the latest submission with a result, in other words it fetches the submission for the most recent commit
     * that has a result.
     *
     * @return the submission or empty if there is no submission or result yet
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public Optional<ProgrammingSubmission> getLatestSubmissionWithResult() throws ArtemisNetworkException {
        return ProgrammingSubmissionDTO.getLatestSubmissionWithResult(
                        this.getConnection().getClient(), this.getId())
                .map(dto -> new ProgrammingSubmission(dto, (ProgrammingExercise) this.exercise, this));
    }

    public Exercise getExercise() {
        return this.exercise;
    }

    /**
     * Resets the repository back to the template.
     * <p>
     * This is not a deletion: it keeps the participation, but replaces the repository
     * contents with the template or graded participation source.
     *
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public void resetRepository() throws ArtemisNetworkException {
        ParticipationDTO.resetRepository(this.getConnection().getClient(), this.getId(), null);
    }

    /**
     * The student can only be retrieved by instructors.
     */
    public Optional<User> getStudent() {
        return Optional.ofNullable(this.dto.student()).map(User::new);
    }

    /**
     * The VCS access token of a participation can be used to access the repository of the participation, e.g. for cloning or pushing.
     * <p>
     * It is limited to the participation, so it can not be used to e.g. make api requests or as a login for artemis.
     *
     * @return the access token
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public String getVcsAccessToken() throws ArtemisNetworkException {
        return ParticipationDTO.getVcsAccessToken(this.getConnection().getClient(), this.getId());
    }

    /**
     * Fetches this participation with all submissions, results and feedbacks.
     * <p>
     * Requires at least student permissions and access to the participation.
     * This is the full programming-exercise variant of the participation response;
     * unlike {@link #getLatestSubmissionWithResult()}, it includes the complete
     * submission history instead of only the latest result.
     * <p>
     * For submissions where no result is available, it will still return a submission.
     *
     * @return a list of programming submissions with their results
     * @throws ArtemisNetworkException if it failed to send the request
     */
    public List<ProgrammingSubmissionWithResults> fetchAllSubmissionsWithResults() throws ArtemisNetworkException {
        var participation = ParticipationDTO.getParticipationWithAllResults(
                this.getConnection().getClient(), this.getId());

        if (participation.submissions() == null) {
            return List.of();
        }

        List<ProgrammingSubmissionWithResults> submissions = new ArrayList<>();
        for (var submission : participation.submissions()) {
            submissions.add(new ProgrammingSubmissionWithResults(
                    new ProgrammingSubmission(submission, (ProgrammingExercise) this.exercise)));
        }

        return submissions;
    }

    protected ParticipationDTO toDTO() {
        return this.dto;
    }

    @Override
    public String toString() {
        return this.getParticipantIdentifier();
    }
}
