/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.grading.artemis;

import edu.kit.kastel.sdq.artemis4j.api.artemis.assessment.Feedback;
import edu.kit.kastel.sdq.artemis4j.api.grading.IAnnotation;
import edu.kit.kastel.sdq.artemis4j.api.grading.IMistakeType;
import edu.kit.kastel.sdq.artemis4j.grading.model.annotation.Annotation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Deserialize Annotation from a Feedback of type: MANUAL_UNREFERENCED, text:
 * CLIENT_DATA, detailText: $THE_JSON_BLOB
 */
public class AnnotationDeserializer {

	private static final Logger log = LoggerFactory.getLogger(AnnotationDeserializer.class);

	private static final String FEEDBACK_TEXT = "CLIENT_DATA";
	private final Map<String, IMistakeType> mistakeTypesMap;
	private final ObjectMapper oom;

	public AnnotationDeserializer(List<IMistakeType> mistakeTypes) {
		this.oom = new ObjectMapper();
		this.mistakeTypesMap = new HashMap<>();
		mistakeTypes.forEach(mistakeType -> this.mistakeTypesMap.put(mistakeType.getIdentifier(), mistakeType));
	}

	/**
	 * Deserialize a given Collection of Feedback (that contain json blobs in the
	 * detailText field) into our model Annotations.
	 */
	public List<IAnnotation> deserialize(List<Feedback> feedbacks) throws IOException {
		final List<Feedback> feedbacksWithAnnotationInformation = feedbacks.stream() //
				.filter(Objects::nonNull) //
				.filter(it -> FEEDBACK_TEXT.equals(it.getCodeLocationHumanReadable())) //
				.toList();

		final List<Annotation> annotations = readAnnotations(feedbacksWithAnnotationInformation);

		for (Annotation annotation : annotations) {
			final String mistakeTypeName = annotation.getMistakeTypeId();
			if (!this.mistakeTypesMap.containsKey(mistakeTypeName)) {
				throw new IOException("Trying to deserialize MistakeType \"" + mistakeTypeName + "\". It was not found in local config!");
			}
			annotation.setMistakeType(this.mistakeTypesMap.get(mistakeTypeName));
		}

		return new ArrayList<>(annotations);
	}

	private List<Annotation> readAnnotations(List<Feedback> feedbacksWithAnnotationInformation) {
		List<Annotation> annotations = new ArrayList<>();
		for (var feedback : feedbacksWithAnnotationInformation) {
			try {
				List<Annotation> annotationsInFeedback = oom.readValue(feedback.getDetailText(), new TypeReference<>() {
				});
				annotations.addAll(annotationsInFeedback);
			} catch (JsonProcessingException e) {
				log.error(e.getMessage(), e);
			}
		}
		return annotations;
	}
}
