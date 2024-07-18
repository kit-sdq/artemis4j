/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j;

public class ArtemisClientException extends Exception {
    public ArtemisClientException() {
    }

    public ArtemisClientException(String message) {
        super(message);
    }

    public ArtemisClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public ArtemisClientException(Throwable cause) {
        super(cause);
    }
}
