/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.git;

import java.time.ZonedDateTime;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cloning strategy that uses a personal access token (PAT) to clone a repository.
 */
public class VCSTokenCloningStrategy implements CloningStrategy {
    private static final Logger log = LoggerFactory.getLogger(VCSTokenCloningStrategy.class);

    private final CredentialsProvider credentialsProvider;

    public VCSTokenCloningStrategy(@Nullable String tokenOverride, ArtemisConnection connection)
            throws ArtemisNetworkException {
        var assessor = connection.getAssessor();

        String token;
        if (tokenOverride != null) {
            token = tokenOverride;
        } else if (assessor.getGitToken().isPresent()) {
            if (assessor.getGitTokenExpiryDate()
                    .orElseThrow()
                    .isBefore(ZonedDateTime.now().plusDays(2))) {
                token = generateNewToken(connection);
            } else {
                token = assessor.getGitToken().orElseThrow();
            }
        } else {
            // The user has not set a token, so create one for him with a default expiration time
            token = generateNewToken(connection);
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

    private String generateNewToken(ArtemisConnection connection) throws ArtemisNetworkException {
        log.info("Generating new VCS access token");
        connection.getAssessor();
        // The max expiration date is (today + 1 year), see
        // https://github.com/ls1intum/Artemis/blob/921b1884fb1a3028512e61023f254f1365fec14b/src/main/java/de/tum/in/www1/artemis/web/rest/AccountResource.java#L175
        var expiryDate = ZonedDateTime.now().plusMonths(6);
        UserDTO.createVCSToken(expiryDate, connection.getClient());
        var assessor = connection.refreshAssessor();
        return assessor.getGitToken().orElseThrow(() -> new IllegalStateException("No VCS token created"));
    }
}
