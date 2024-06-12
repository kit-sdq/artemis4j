package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.client.SubmissionDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.IOException;
import java.nio.file.Path;

/**
 * A student's programming submission. A submission essentially consists of the URL of a student's Git repository, along with a commit hash.
 * We do not model the separate 'participation' entity in Artemis.
 */
public class Submission extends ArtemisConnectionHolder {
    private final SubmissionDTO dto;

    private final Exercise exercise;

    public Submission(SubmissionDTO dto, Exercise exercise) {
        super(exercise);

        this.dto = dto;
        this.exercise = exercise;
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

    public Exercise getExercise() {
        return exercise;
    }

    /**
     * Clones the submission, including the test repository, into the given path, and checks out the submitted commit.
     * @param target The path to clone into
     * @param tokenOverride (optional) The git password to use for cloning
     * @return The path to the actual submission within the target location
     * @throws ArtemisClientException
     */
    public Path cloneInto(Path target, String tokenOverride) throws ArtemisClientException {
        // Clone the test repository
        this.cloneRepositoryInto(this.getExercise().getTestRepositoryUrl(), target, tokenOverride);

        // Clone the student's submission into a subfolder
        Path assignmentPath = target.resolve("assignment");
        this.cloneRepositoryInto(this.getRepositoryUrl(), assignmentPath, tokenOverride);

        // Check out the submitted commit
        try (var repo = Git.open(assignmentPath.toFile())){
            repo.checkout()
                    .setName(this.getCommitHash())
                    .call();
        } catch (GitAPIException | IOException e) {
            throw new ArtemisClientException("Failed to check out the submitted commit", e);
        }
        return assignmentPath;
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