/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.git;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import javax.swing.*;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloning strategy that uses SSH to clone a repository.
 */
public class SSHCloningStrategy implements CloningStrategy {
    private static final Logger log = LoggerFactory.getLogger(SSHCloningStrategy.class);

    private final CredentialsProvider credentialsProvider;

    public SSHCloningStrategy(ArtemisConnection connection)
            throws ArtemisNetworkException, UnsupportedCloningStrategyException {
        if (connection.getAssessor().getGitSSHKey().isEmpty()) {
            throw new UnsupportedCloningStrategyException("Cannot clone via SSH - no SSH key set");
        }

        if (connection.getManagementInfo().sshCloneURLTemplate() == null) {
            throw new UnsupportedCloningStrategyException(
                    "Cannot clone via SSH - Artemis instance does not support SSH cloning");
        }

        this.credentialsProvider = new InteractiveCredentialsProvider();
    }

    @Override
    public void performClone(String repositoryUrl, CloneCommand command, ArtemisConnection connection)
            throws ArtemisNetworkException, GitAPIException {
        String sshTemplate = connection.getManagementInfo().sshCloneURLTemplate();
        String sshUrl = createSSHUrl(repositoryUrl, sshTemplate);
        log.info("Cloning repository via SSH from {}", sshUrl);

        var sshdFactoryBuilder = new SshdSessionFactoryBuilder()
                .setHomeDirectory(FS.DETECTED.userHome())
                .setSshDirectory(new File(FS.DETECTED.userHome(), "/.ssh"))
                .setPreferredAuthentications("publickey");

        try (var sshdFactory = sshdFactoryBuilder.build(null)) {
            SshSessionFactory.setInstance(sshdFactory);
            command.setTransportConfigCallback((transport -> {
                        if (transport instanceof SshTransport sshTransport) {
                            sshTransport.setSshSessionFactory(sshdFactory);
                        }
                    }))
                    .setCredentialsProvider(this.credentialsProvider)
                    .setURI(sshUrl)
                    .call()
                    .close();
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
                    int result = JOptionPane.showConfirmDialog(
                            null, yesNoItem.getPromptText(), "Clone via SSH", JOptionPane.YES_NO_CANCEL_OPTION);
                    switch (result) {
                        case JOptionPane.YES_OPTION -> yesNoItem.setValue(true);
                        case JOptionPane.NO_OPTION -> yesNoItem.setValue(false);
                        case JOptionPane.CANCEL_OPTION -> {
                            return false;
                        }
                    }
                } else if (item instanceof CredentialItem.Password passwordItem) {
                    if (this.passphrase == null) {
                        this.askForPassword(passwordItem);
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

        private void askForPassword(CredentialItem.Password passwordItem) {
            try {
                SwingUtilities.invokeAndWait(
                        () -> this.passphrase = PasswordPanel.show("Clone via SSH", passwordItem.getPromptText())
                                .orElse(null));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
