/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ExerciseGroupDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty boolean isMandatory,
        @JsonProperty List<ProgrammingExerciseDTO> exercises) {}
