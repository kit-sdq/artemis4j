/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record ProgrammingExerciseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty String testRepositoryUri,
        @JsonProperty @Nullable Boolean secondCorrectionEnabled,
        @JsonProperty String exerciseType,
        @JsonProperty String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty ZonedDateTime dueDate,
        @JsonProperty ZonedDateTime startDate) {

    public static List<ProgrammingExerciseDTO> fetchAll(ArtemisClient client, int courseId)
            throws ArtemisNetworkException {
        var exercises = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class);
        // Remove all non-programming exercises
        return exercises.exercises().stream()
                .filter(e -> e.exerciseType().equalsIgnoreCase("programming"))
                .toList();
    }

    /**
     * Artemis doesn't return the list of exercises directly, but only the list a
     * specific course with the exercises attached. We don't care about the course
     * here, so this wrapper class exists.
     *
     * @param exercises
     */
    private record ExerciseWrapperDTO(List<ProgrammingExerciseDTO> exercises) {
        private ExerciseWrapperDTO {
            if (exercises == null) {
                exercises = List.of();
            }
        }
    }
}
