package edu.kit.kastel.sdq.artemis4j.metajson;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is not part of the Artemis API. We save it as JSON in a MANUAL_UNREFERENCED feedback and deserialize it for grading.
 */
public record AnnotationDTO(
        @JsonProperty String uuid,
        @JsonProperty String mistakeTypeId,
        @JsonProperty int startLine,
        @JsonProperty int endLine,
        @JsonProperty String classFilePath,
        @JsonProperty String customMessageForJSON,
        @JsonProperty Double customPenaltyForJSON
) {
}