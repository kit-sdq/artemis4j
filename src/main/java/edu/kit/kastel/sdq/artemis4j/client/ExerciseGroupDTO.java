package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExerciseGroupDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty boolean isMandatory,
        @JsonProperty List<ProgrammingExerciseDTO> exercises
) {
}
