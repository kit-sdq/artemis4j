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

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a submission and associated tests repository that has been cloned to a local folder.
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

        // Create only one userInfo to cache credentials between both clones
        var userInfo = new SwingUserInfo();

        // Clone the test repository
        cloneRepositoryInto(submission.getExercise().getTestRepositoryUrl(), target, tokenOverride, userInfo, connection);

        // Clone the student's submission into a subfolder
        Path submissionPath = target.resolve("assignment");
        cloneRepositoryInto(submission.getRepositoryUrl(), submissionPath, tokenOverride, userInfo, connection);

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

    private static void cloneRepositoryInto(String repositoryURL, Path target, String tokenOverride, UserInfo userInfo, ArtemisConnection connection)
            throws ArtemisClientException {
        var assessor = connection.getAssessor();

        CloneCommand cloneCommand = Git.cloneRepository().setDirectory(target.toAbsolutePath().toFile()).setRemote("origin").setURI(repositoryURL)
                .setCloneAllBranches(true).setCloneSubmodules(false);

        if (userInfo != null && assessor.getGitSSHKey().isPresent()) {
            String sshTemplate = connection.getManagementInfo().sshCloneURLTemplate();
            if (sshTemplate == null) {
                throw new IllegalStateException("SSH key is set, but the Artemis instance does not support SSH cloning");
            }

            String sshUrl = createSSHUrl(repositoryURL, sshTemplate);
            log.info("Cloning repository via SSH from {}", sshUrl);
            cloneCommand.setTransportConfigCallback(new SshTransportConfigCallback(userInfo)).setURI(sshUrl);
        } else {
            log.info("Cloning repository via HTTPS from {}", repositoryURL);
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
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(assessor.getLogin(), token)).setURI(repositoryURL);
        }

        try {
            cloneCommand.call().close();
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
        return url.replaceAll("^\\w*://[^/]*?/(scm/)?(.*)$", sshTemplate + "$2");
    }

    private static class SshTransportConfigCallback implements TransportConfigCallback {
        private final SshSessionFactory sshSessionFactory;

        public SshTransportConfigCallback(UserInfo userInfo) {
            this.sshSessionFactory = new JschConfigSessionFactory() {
                @Override
                protected void configure(OpenSshConfig.Host hc, Session session) {
                    session.setUserInfo(userInfo);
                }

                @Override
                protected JSch createDefaultJSch(FS fs) throws JSchException {
                    JSch jsch = super.createDefaultJSch(fs);
                    jsch.setConfigRepository(OpenSshConfig.get(fs));

                    String knownHostsFile = System.getProperty("user.home") + File.separator + ".ssh" + File.separator + "known_hosts";
                    if (new File(knownHostsFile).exists()) {
                        jsch.setKnownHosts(knownHostsFile);
                    }

                    return jsch;
                }
            };
        }

        @Override
        public void configure(Transport transport) {
            SshTransport sshTransport = (SshTransport) transport;
            sshTransport.setSshSessionFactory(sshSessionFactory);
        }
    }

    private static class SwingUserInfo implements UserInfo {
        private String password;
        private String passphrase;

        @Override
        public String getPassphrase() {
            return this.passphrase;
        }

        @Override
        public String getPassword() {
            return this.password;
        }

        @Override
        public boolean promptPassword(String message) {
            if (this.password == null) {
                this.password = PasswordPanel.show("Clone via SSH", "Enter SSH Password").orElse(null);
            }

            return this.password != null;
        }

        @Override
        public boolean promptPassphrase(String message) {
            if (this.passphrase == null) {
                this.passphrase = PasswordPanel.show("Clone via SSH", "Enter SSH Key Passphrase").orElse(null);
            }

            return this.passphrase != null;
        }

        @Override
        public boolean promptYesNo(String message) {
            return JOptionPane.showOptionDialog(null, message, "SSH", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                    new Object[]{"yes", "no"}, "yes") == JOptionPane.YES_OPTION;
        }

        @Override
        public void showMessage(String message) {
            JOptionPane.showMessageDialog(null, message);
        }
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
}
