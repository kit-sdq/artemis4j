package edu.kit.kastel.sdq.artemis4j.new_client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;

public class AnnotationMappingException extends ArtemisClientException {
    public AnnotationMappingException(String message) {
        super(message);
    }

    public AnnotationMappingException(Throwable cause) {
        super(cause);
    }

    public AnnotationMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
