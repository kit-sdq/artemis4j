/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.grading.git.CloningStrategy;
import edu.kit.kastel.sdq.artemis4j.grading.git.SSHCloningStrategy;
import edu.kit.kastel.sdq.artemis4j.grading.git.VCSTokenCloningStrategy;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a submission and associated tests repository that has been cloned
 * to a local folder.
 * <p>
 * Most of the logic in this class is required to support cloning via SSH.
 */
public class ClonedProgrammingSubmission implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(ClonedProgrammingSubmission.class);
    private final ProgrammingSubmission submission;
    private final Path testsPath;
    private final Path submissionPath;

    static ClonedProgrammingSubmission cloneSubmissionViaSSH(ProgrammingSubmission submission, Path target)
            throws ArtemisClientException {
        var strategy = new SSHCloningStrategy(submission.getConnection());
        return cloneSubmissionInternal(submission, target, strategy);
    }

    static ClonedProgrammingSubmission cloneSubmissionViaVCSToken(
            ProgrammingSubmission submission, Path target, String tokenOverride) throws ArtemisClientException {
        var strategy = new VCSTokenCloningStrategy(tokenOverride, submission.getConnection());
        return cloneSubmissionInternal(submission, target, strategy);
    }

    private static ClonedProgrammingSubmission cloneSubmissionInternal(
            ProgrammingSubmission submission, Path target, CloningStrategy strategy) throws ArtemisClientException {
        var connection = submission.getConnection();

        // Clone the test repository
        cloneRepositoryInto(submission.getExercise().getTestRepositoryUrl(), target, strategy, connection);

        // Clone the student's submission into a subfolder
        Path submissionPath = target.resolve("assignment");
        cloneRepositoryInto(submission.getRepositoryUrl(), submissionPath, strategy, connection);

        // Check out the submitted commit
        try (var repo = Git.open(submissionPath.toFile())) {
            repo.checkout().setName(submission.getCommitHash()).call();
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

    private static void cloneRepositoryInto(
            String repositoryURL, Path target, CloningStrategy strategy, ArtemisConnection connection)
            throws ArtemisClientException {

        CloneCommand cloneCommand = Git.cloneRepository()
                .setDirectory(target.toAbsolutePath().toFile())
                .setRemote("origin")
                .setCloneAllBranches(true)
                .setCloneSubmodules(false);

        try {
            strategy.performClone(repositoryURL, cloneCommand, connection);
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
            dirStream.map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
        }
    }
}
