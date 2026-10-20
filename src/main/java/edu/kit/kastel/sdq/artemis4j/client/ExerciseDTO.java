/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        defaultImpl = UnknownExerciseDTO.class)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProgrammingExerciseDTO.class, name = "programming"),
    @JsonSubTypes.Type(value = TextExerciseDTO.class, name = "text"),
})
public sealed interface ExerciseDTO permits ProgrammingExerciseDTO, TextExerciseDTO, UnknownExerciseDTO {
    /**
     * Fetches all exercises for a course. Jackson maps each entry to its concrete DTO based on its {@code type}.
     */
    static List<ExerciseDTO> fetchAll(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class)
                .exercises();
    }
}

record ExerciseWrapperDTO(List<ExerciseDTO> exercises) {
    ExerciseWrapperDTO {
        exercises = exercises == null ? List.of() : List.copyOf(exercises);
    }
}
