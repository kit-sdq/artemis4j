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
        @JsonProperty @Nullable String shortName,
        @JsonProperty @Nullable String testRepositoryUri,
        @JsonProperty @Nullable Boolean secondCorrectionEnabled,
        @JsonProperty @Nullable ExerciseType exerciseType,
        @JsonProperty @Nullable String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty @Nullable ZonedDateTime dueDate,
        @JsonProperty @Nullable ZonedDateTime startDate)
        implements ExerciseDTO {

    public static ProgrammingExerciseDTO fetchWithTemplateAndSolutionParticipation(
            ArtemisClient client, long exerciseId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of(
                        "programming", "programming-exercises", exerciseId, "with-template-and-solution-participation"))
                .executeAndDecode(client, ProgrammingExerciseDTO.class);
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
}
