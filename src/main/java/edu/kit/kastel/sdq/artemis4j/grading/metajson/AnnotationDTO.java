/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading.metajson;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;
import edu.kit.kastel.sdq.artemis4j.grading.location.LineColumn;

/**
 * This class is not part of the Artemis API. We save it as JSON in a
 * MANUAL_UNREFERENCED feedback and deserialize it for grading.
 */
public record AnnotationDTO(
        @JsonProperty String uuid,
        @JsonProperty String mistakeTypeId,
        @JsonProperty LineColumn start,
        @JsonProperty LineColumn end,
        @JsonProperty String classFilePath,
        @JsonProperty String customMessageForJSON,
        @JsonProperty Double customPenaltyForJSON,
        @JsonProperty AnnotationSource source,
        @JsonProperty List<String> classifiers,
        @JsonProperty Integer annotationLimit,
        @JsonProperty Long createdByUserId,
        @JsonProperty Long suppressedByUserId) {}
