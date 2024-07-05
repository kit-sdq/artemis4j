package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class ClonedProgrammingSubmission implements AutoCloseable {
    private final ProgrammingSubmission submission;
    private final Path testsPath;
    private final Path submissionPath;

    /* package-private */ static ClonedProgrammingSubmission cloneSubmission(ProgrammingSubmission submission, Path target, String tokenOverride) throws ArtemisClientException {
        var connection = submission.getConnection();

        // Clone the test repository
        cloneRepositoryInto(submission.getExercise().getTestRepositoryUrl(), target, tokenOverride, connection);

        // Clone the student's submission into a subfolder
        Path submissionPath = target.resolve("assignment");
        cloneRepositoryInto(submission.getRepositoryUrl(), submissionPath, tokenOverride, connection);

        // Check out the submitted commit
        try (var repo = Git.open(submissionPath.toFile())){
            repo.checkout()
                    .setName(submission.getCommitHash())
                    .call();
        } catch (GitAPIException | IOException e) {
            throw new ArtemisClientException("Failed to check out the submitted commit", e);
        }

        return new ClonedProgrammingSubmission(submission, target, submissionPath);
    }

    private ClonedProgrammingSubmission(ProgrammingSubmission submission, Path testsPath, Path submissionPath) {
        this.submission = submission;
        this.testsPath = testsPath;
        this.submissionPath = submissionPath;
    }

    public ProgrammingSubmission getSubmission() {
        return submission;
    }

    public Path getTestsPath() {
        return testsPath;
    }

    public Path getSubmissionPath() {
        return submissionPath;
    }

    public Path getSubmissionSourcePath() {
        return submissionPath.resolve("src");
    }

    private static void cloneRepositoryInto(String repositoryURL, Path target, String tokenOverride, ArtemisConnection connection) throws ArtemisClientException {
        var assessor = connection.getAssessor();

        String username = assessor.getLogin();
        String token;
        if (tokenOverride != null) {
            token = tokenOverride;
        } else if (assessor.getGitToken().isPresent()) {
            token = assessor.getGitToken().get();
        } else if (connection.getClient().getPassword().isPresent()) {
            token = connection.getClient().getPassword().get();
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
                    .call().close();
        } catch (GitAPIException e) {
            throw new ArtemisClientException("Failed to clone the submission repository", e);
        }
    }

    @Override
    public void close() throws ArtemisClientException {
        try {
            deleteDirectory(this.testsPath);
        } catch (IOException e) {
            throw new ArtemisClientException("Failed to delete the cloned submission", e);
        }
    }

    private static void deleteDirectory(Path path) throws IOException {
        try (var dirStream = Files.walk(path)) {
            dirStream
                    .map(Path::toFile)
                    .sorted(Comparator.reverseOrder())
                    .forEach(File::delete);
        }
    }
}
