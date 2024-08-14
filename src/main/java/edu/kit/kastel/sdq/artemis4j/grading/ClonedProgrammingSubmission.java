/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
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

    static ClonedProgrammingSubmission cloneSubmission(ProgrammingSubmission submission, Path target, String tokenOverride) throws ArtemisClientException {
        var connection = submission.getConnection();

        // Cache credentials between both clones
        var credentialsProvider = buildCredentialsProvider(tokenOverride, connection);

        // Clone the test repository
        cloneRepositoryInto(submission.getExercise().getTestRepositoryUrl(), target, credentialsProvider, connection);

        // Clone the student's submission into a subfolder
        Path submissionPath = target.resolve("assignment");
        cloneRepositoryInto(submission.getRepositoryUrl(), submissionPath, credentialsProvider, connection);

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

    private static CredentialsProvider buildCredentialsProvider(String tokenOverride, ArtemisConnection connection) throws ArtemisNetworkException {
        var assessor = connection.getAssessor();

        if (tokenOverride == null && assessor.getGitSSHKey().isPresent()) {
            return new InteractiveCredentialsProvider();
        } else {
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
            return new UsernamePasswordCredentialsProvider(assessor.getLogin(), token);
        }
    }

    private static void cloneRepositoryInto(String repositoryURL, Path target, CredentialsProvider credentialsProvider, ArtemisConnection connection)
            throws ArtemisClientException {
        var assessor = connection.getAssessor();

        CloneCommand cloneCommand = Git.cloneRepository().setDirectory(target.toAbsolutePath().toFile()).setRemote("origin").setURI(repositoryURL)
                .setCloneAllBranches(true).setCloneSubmodules(false).setCredentialsProvider(credentialsProvider);

        try {
            if (assessor.getGitSSHKey().isPresent()) {
                String sshTemplate = connection.getManagementInfo().sshCloneURLTemplate();
                if (sshTemplate == null) {
                    throw new IllegalStateException("SSH key is set, but the Artemis instance does not support SSH cloning");
                }

                String sshUrl = createSSHUrl(repositoryURL, sshTemplate);
                log.info("Cloning repository via SSH from {}", sshUrl);

                var sshdFactoryBuilder = new SshdSessionFactoryBuilder().setHomeDirectory(FS.DETECTED.userHome())
                        .setSshDirectory(new File(FS.DETECTED.userHome(), "/.ssh")).setPreferredAuthentications("publickey");

                try (var sshdFactory = sshdFactoryBuilder.build(null)) {
                    SshSessionFactory.setInstance(sshdFactory);
                    cloneCommand.setTransportConfigCallback((transport -> {
                        if (transport instanceof SshTransport sshTransport) {
                            sshTransport.setSshSessionFactory(sshdFactory);
                        }
                    })).setURI(sshUrl).call().close();
                }
            } else {
                log.info("Cloning repository via HTTPS from {}", repositoryURL);
                cloneCommand.setURI(repositoryURL).call().close();
            }

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

    private static String createSSHUrl(String url, String sshTemplate) {
        // Based on Artemis' getSshCloneUrl method
        // https://github.com/ls1intum/Artemis/blob/eb5b9bd4321d953217e902868ac9f38de6dd6c6f/src/main/webapp/app/shared/components/code-button/code-button.component.ts#L174
        return url.replaceAll("^\\w*://[^/]*?/(scm/)?(.*)$", sshTemplate + "$2");
    }

    private static final class PasswordPanel extends JPanel {
        private final JPasswordField passwordField = new JPasswordField();

        public PasswordPanel(String prompt) {
            super();
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.add(new JLabel(prompt));
            this.add(passwordField);
        }

        public static Optional<String> show(String title, String prompt) {
            PasswordPanel panel = new PasswordPanel(prompt);
            JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
                @Override
                public void selectInitialValue() {
                    panel.passwordField.requestFocusInWindow();
                }
            };
            JDialog dialog = pane.createDialog(title);
            dialog.setVisible(true);

            if (pane.getValue() != null && pane.getValue().equals(JOptionPane.OK_OPTION)) {
                return Optional.of(new String(panel.passwordField.getPassword()));
            }
            return Optional.empty();
        }
    }

    private static final class InteractiveCredentialsProvider extends CredentialsProvider {
        private String passphrase;

        @Override
        public boolean isInteractive() {
            return true;
        }

        @Override
        public boolean supports(CredentialItem... items) {
            return true;
        }

        @Override
        public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
            for (var item : items) {
                if (item instanceof CredentialItem.YesNoType yesNoItem) {
                    int result = JOptionPane.showConfirmDialog(null, yesNoItem.getPromptText(), "Clone via SSH", JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result) {
                    case JOptionPane.YES_OPTION -> yesNoItem.setValue(true);
                    case JOptionPane.NO_OPTION -> yesNoItem.setValue(false);
                    case JOptionPane.CANCEL_OPTION -> {
                        return false;
                    }
                    }
                } else if (item instanceof CredentialItem.Password passwordItem) {
                    if (this.passphrase == null) {
                        this.passphrase = PasswordPanel.show("Clone via SSH", passwordItem.getPromptText()).orElse(null);
                    }

                    if (this.passphrase != null) {
                        passwordItem.setValueNoCopy(passphrase.toCharArray());
                    } else {
                        return false;
                    }
                }
            }
            return true;
        }
    }
}
