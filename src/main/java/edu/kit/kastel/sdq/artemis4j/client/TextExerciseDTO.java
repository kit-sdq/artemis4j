/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record TextExerciseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty @Nullable String shortName,
        @JsonProperty @Nullable ExerciseType exerciseType,
        @JsonProperty @Nullable String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty double bonusPoints,
        @JsonProperty @Nullable ZonedDateTime dueDate,
        @JsonProperty @Nullable ZonedDateTime startDate)
        implements ExerciseDTO {}
