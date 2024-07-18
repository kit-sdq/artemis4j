/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.penalty;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

public class InvalidGradingConfigException extends ArtemisClientException {
    public InvalidGradingConfigException(String message) {
        super(message);
    }

    public InvalidGradingConfigException(Throwable cause) {
        super(cause);
    }

    public InvalidGradingConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
