/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record ExamExerciseDTO(
        @JsonProperty long id,
        @JsonProperty ExerciseType type,
        @JsonProperty @Nullable String title,
        @JsonProperty @Nullable Double maxPoints,
        @JsonProperty @Nullable String shortName,
        @JsonProperty @Nullable AssessmentType assessmentType) {}
