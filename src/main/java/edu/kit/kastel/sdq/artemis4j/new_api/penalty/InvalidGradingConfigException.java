package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;

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
