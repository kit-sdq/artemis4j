/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading.metajson;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;

/**
 * This class is not part of the Artemis API. We save it as JSON in a
 * MANUAL_UNREFERENCED feedback and deserialize it for grading.
 */
public record AnnotationDTO(@JsonProperty String uuid, @JsonProperty String mistakeTypeId, @JsonProperty int startLine, @JsonProperty int endLine,
        @JsonProperty String classFilePath, @JsonProperty String customMessageForJSON, @JsonProperty Double customPenaltyForJSON,
        @JsonProperty AnnotationSource source) {
}
