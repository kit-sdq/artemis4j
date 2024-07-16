/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.autograder;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

public class AutograderFailedException extends ArtemisClientException {
    public AutograderFailedException(String message) {
        super(message);
    }

    public AutograderFailedException(Throwable cause) {
        super("Autograder failed", cause);
    }

    public AutograderFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
