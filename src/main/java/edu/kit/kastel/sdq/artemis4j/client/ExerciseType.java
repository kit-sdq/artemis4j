/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ExerciseType {
    @JsonProperty("text")
    TEXT,

    @JsonProperty("programming")
    PROGRAMMING,

    @JsonProperty("modeling")
    MODELING,

    @JsonProperty("file-upload")
    FILE_UPLOAD,

    @JsonProperty("quiz")
    QUIZ,
}
