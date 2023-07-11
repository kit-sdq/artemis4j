/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.annotation;

import java.io.Serial;

public class AnnotationException extends Exception {

	@Serial
	private static final long serialVersionUID = -6367050657799038594L;

	public AnnotationException(String message) {
		super(message);
	}

}
