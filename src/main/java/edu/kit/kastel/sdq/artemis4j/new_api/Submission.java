package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.nio.file.Path;

public class Submission extends ArtemisClientHolder {
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
     * Clones the submission, including the test repository, into the given path
     * @param target The path to clone into
     * @param tokenOverride (optional) The git password to use for cloning
     * @return The path to the actual submission within the target location
     * @throws ArtemisClientException
     */
    public Path cloneInto(Path target, String tokenOverride) throws ArtemisClientException {
        this.cloneRepositoryInto(this.getExercise().getTestRepositoryUrl(), target, tokenOverride);
        Path assignmentPath = target.resolve("assignment");
        this.cloneRepositoryInto(this.getRepositoryUrl(), assignmentPath, tokenOverride);
        return assignmentPath;
    }

    private void cloneRepositoryInto(String repositoryURL, Path target, String tokenOverride) throws ArtemisClientException {
        var assessor = this.getClient().getAssessor();

        String username = assessor.getLogin();
        String token;
        if (tokenOverride != null) {
            token = tokenOverride;
        } else if (assessor.getGitToken().isPresent()) {
            token = assessor.getGitToken().get();
        } else if (this.getClient().getPassword().isPresent()) {
            token = this.getClient().getPassword().get();
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
