package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.client.SubmissionDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * A student's programming submission. A submission essentially consists of the URL of a student's Git repository, along with a commit hash.
 * We do not model the separate 'participation' entity in Artemis.
 */
public class Submission extends ArtemisConnectionHolder {
    private final SubmissionDTO dto;
    private final User student;

    private final Exercise exercise;

    public Submission(SubmissionDTO dto, Exercise exercise) {
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
    }

    public long getId() {
        return this.dto.id();
    }

    public long getParticipationId() {
        return this.dto.participation().id();
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
     * @return
     */
    public Optional<User> getStudent() {
        return Optional.ofNullable(this.student);
    }

    public Exercise getExercise() {
        return exercise;
    }

    /**
     * Clones the submission, including the test repository, into the given path, and checks out the submitted commit.
     *
     * @param target        The path to clone into
     * @param tokenOverride (optional) The git password to use for cloning
     * @return The path to the actual submission within the target location
     * @throws ArtemisClientException
     */
    public ClonedSubmission cloneInto(Path target, String tokenOverride) throws ArtemisClientException {
        return ClonedSubmission.cloneSubmission(this, target, tokenOverride);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Submission that = (Submission) o;
        return this.getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.getId());
    }

    private void cloneRepositoryInto(String repositoryURL, Path target, String tokenOverride) throws ArtemisClientException {
        var assessor = this.getConnection().getAssessor();

        String username = assessor.getLogin();
        String token;
        if (tokenOverride != null) {
            token = tokenOverride;
        } else if (assessor.getGitToken().isPresent()) {
            token = assessor.getGitToken().get();
        } else if (this.getConnection().getClient().getPassword().isPresent()) {
            token = this.getConnection().getClient().getPassword().get();
        } else {
            token = "";
        }

        try {
            Git.cloneRepository()
                    .setDirectory(target.toAbsolutePath().toFile())
                    .setRemote("origin")
                    .setURI(repositoryURL)
                    .setCloneAllBranches(true)
                    .setCloneSubmodules(false)
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, token))
                    .call();
        } catch (GitAPIException e) {
            throw new ArtemisClientException("Failed to clone the submission repository", e);
        }
    }
}
