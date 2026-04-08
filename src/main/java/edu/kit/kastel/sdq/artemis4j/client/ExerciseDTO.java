/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true,
        defaultImpl = UnknownExerciseDTO.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProgrammingExerciseDTO.class, name = "programming"),
    @JsonSubTypes.Type(value = TextExerciseDTO.class, name = "text"),
})
public sealed interface ExerciseDTO permits ProgrammingExerciseDTO, TextExerciseDTO, UnknownExerciseDTO {}
