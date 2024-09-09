/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.git;

import java.time.ZonedDateTime;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloning strategy that uses a personal access token (PAT) to clone a repository.
 */
public class VCSTokenCloningStrategy implements CloningStrategy {
    private static final Logger log = LoggerFactory.getLogger(VCSTokenCloningStrategy.class);

    private final CredentialsProvider credentialsProvider;

    public VCSTokenCloningStrategy(String tokenOverride, ArtemisConnection connection) throws ArtemisNetworkException {
        var assessor = connection.getAssessor();

        String token;
        if (tokenOverride != null) {
            token = tokenOverride;
        } else if (assessor.getGitToken().isPresent()) {
            token = assessor.getGitToken().get();
        } else {
            // The user has not set a token, so create one for him with a default expiration time
            log.info("Creating new VCS access token");
            var expiryDate = ZonedDateTime.now().plusDays(5);
            UserDTO.createVCSToken(expiryDate, connection.getClient());

            // Refresh the assessor to get the new token
            assessor = connection.refreshAssessor();
            token = assessor.getGitToken().orElseThrow(() -> new IllegalStateException("No VCS token created"));
        }

        this.credentialsProvider = new UsernamePasswordCredentialsProvider(assessor.getLogin(), token);
    }

    @Override
    public void performClone(String repositoryUrl, CloneCommand command, ArtemisConnection connection)
            throws ArtemisNetworkException, GitAPIException {
        log.info("Cloning repository via HTTPS from {}", repositoryUrl);
        command.setURI(repositoryUrl)
                .setCredentialsProvider(this.credentialsProvider)
                .call()
                .close();
    }
}
