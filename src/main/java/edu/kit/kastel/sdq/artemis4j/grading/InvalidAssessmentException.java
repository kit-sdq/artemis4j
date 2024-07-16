/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisClientException;

public class InvalidAssessmentException extends ArtemisClientException {
    public InvalidAssessmentException(String message) {
        super(message);
    }
}
