/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.git;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

public interface CloningStrategy {
    /**
     * Clones a repository using the given command and connection.
     *
     * @param repositoryUrl The (HTTPS) URL of the repository to clone.
     * @param command       Preconfigured clone command. Strategy implementations must set the URI and everything authentication-related.
     */
    void performClone(String repositoryUrl, CloneCommand command, ArtemisConnection connection)
            throws ArtemisNetworkException, GitAPIException;
}
