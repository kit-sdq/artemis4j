/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.git;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

public class UnsupportedCloningStrategyException extends ArtemisClientException {
    public UnsupportedCloningStrategyException(String message) {
        super(message);
    }

    public UnsupportedCloningStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
