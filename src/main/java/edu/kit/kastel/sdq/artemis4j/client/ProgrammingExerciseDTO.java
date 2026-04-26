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
        @JsonProperty ZonedDateTime startDate)
        implements ExerciseDTO {

    public static List<ProgrammingExerciseDTO> fetchAll(ArtemisClient client, long courseId)
            throws ArtemisNetworkException {
        var exercises = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class);
        // Remove all non-programming exercises
        return exercises.exercises().stream()
                .filter(e -> e instanceof ProgrammingExerciseDTO)
                .map(e -> (ProgrammingExerciseDTO) e)
                .toList();
    }

    public static ProgrammingExerciseDTO create(
            ArtemisClient client, ProgrammingExerciseCreateDTO exerciseCreateDTO, boolean emptyRepositories)
            throws ArtemisNetworkException {
        return ArtemisRequest.post()
                .path(List.of("programming", "programming-exercises", "setup"))
                .param("emptyRepositories", emptyRepositories)
                .body(exerciseCreateDTO)
                .executeAndDecode(client, ProgrammingExerciseDTO.class);
    }

    public static void delete(ArtemisClient client, long exerciseId, boolean deleteBaseReposBuildPlans)
            throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("programming", "programming-exercises", exerciseId))
                .param("deleteBaseReposBuildPlans", deleteBaseReposBuildPlans)
                .execute(client);
    }

    /**
     * Artemis doesn't return the list of exercises directly, but only the list a
     * specific course with the exercises attached. We don't care about the course
     * here, so this wrapper class exists.
     *
     * @param exercises
     */
    private record ExerciseWrapperDTO(List<ExerciseDTO> exercises) {
        private ExerciseWrapperDTO {
            if (exercises == null) {
                exercises = List.of();
            }
        }
    }
}
