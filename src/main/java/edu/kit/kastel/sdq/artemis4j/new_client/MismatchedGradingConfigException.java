package edu.kit.kastel.sdq.artemis4j.new_client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;

public class MismatchedGradingConfigException extends AnnotationMappingException {
    public MismatchedGradingConfigException(String message) {
        super("Grading config does not match: " + message);
    }

    public MismatchedGradingConfigException(Throwable cause) {
        super(cause);
    }

    public MismatchedGradingConfigException(String message, Throwable cause) {
        super("Grading config does not match: " + message, cause);
    }
}
