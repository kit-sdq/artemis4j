/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;

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
