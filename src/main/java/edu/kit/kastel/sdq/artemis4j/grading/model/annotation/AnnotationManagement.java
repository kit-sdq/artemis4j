/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.model.annotation;

import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;
import edu.kit.kastel.sdq.artemis4j.api.grading.IMistakeType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Holder of Annotation State.
 */
public class AnnotationManagement {

	private final Set<IAnnotation> annotations;

	public AnnotationManagement() {
		this.annotations = new HashSet<>();
	}

	/**
	 * Add an annotation to the current assessment.
	 */
	public void addAnnotation(String annotationId, IMistakeType mistakeType, int startLine, int endLine, String fullyClassifiedClassName, String customMessage,
			Double customPenalty) throws AnnotationException {
		if (this.idExists(annotationId)) {
			throw new AnnotationException("ID " + annotationId + " already exists!");
		}

		this.annotations.add(new Annotation(annotationId, mistakeType, startLine, endLine, fullyClassifiedClassName, customMessage, customPenalty));
	}

	/**
	 * Get an existent annotation by id
	 *
	 * @param annotationId unique annotation identifier
	 * @return the annotation
	 */
	public IAnnotation getAnnotation(String annotationId) {
		return this.annotations.stream().filter(annotation -> annotation.getUUID().equals(annotationId)).findAny().orElseThrow();
	}

	/**
	 * @return all annotations already made for the current assessment.
	 */
	public Set<IAnnotation> getAnnotations() {
		return Collections.unmodifiableSet(this.annotations);
	}

	private boolean idExists(String annotationId) {
		return this.annotations.stream().anyMatch(annotation -> annotation.getUUID().equals(annotationId));
	}

	/**
	 * Modify an annotation in the database.
	 */
	public void modifyAnnotation(String annotationId, String customMessage, Double customPenalty) {
		final IAnnotation oldAnnotation = this.getAnnotation(annotationId);
		final IAnnotation newAnnotation = new Annotation(oldAnnotation.getUUID(), oldAnnotation.getMistakeType(), oldAnnotation.getStartLine(),
				oldAnnotation.getEndLine(), oldAnnotation.getClassFilePath(), customMessage, customPenalty);

		this.annotations.remove(oldAnnotation);
		this.annotations.add(newAnnotation);
	}

	/**
	 * Remove an existent annotation
	 *
	 * @param annotationId unique annotation identifier
	 */
	public void removeAnnotation(String annotationId) {
		if (this.idExists(annotationId)) {
			this.annotations.remove(this.getAnnotation(annotationId));
		}
	}

}
