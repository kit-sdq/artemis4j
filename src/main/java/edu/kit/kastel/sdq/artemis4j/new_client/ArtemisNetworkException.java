package edu.kit.kastel.sdq.artemis4j.new_client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;

public class ArtemisNetworkException extends ArtemisClientException {
    public ArtemisNetworkException(String message) {
        super(message);
    }

    public ArtemisNetworkException(Throwable cause) {
        super(cause);
    }

    public ArtemisNetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}
