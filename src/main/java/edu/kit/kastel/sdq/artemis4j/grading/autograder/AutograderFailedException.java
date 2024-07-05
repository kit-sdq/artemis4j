package edu.kit.kastel.sdq.artemis4j.grading.autograder;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

public class AutograderFailedException extends ArtemisClientException {
    public AutograderFailedException(Throwable cause) {
        super("Autograder failed", cause);
    }
}
