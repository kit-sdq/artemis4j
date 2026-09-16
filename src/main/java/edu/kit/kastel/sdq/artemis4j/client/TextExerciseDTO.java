/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record TextExerciseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty @Nullable String shortName,
        @JsonProperty String exerciseType,
        @JsonProperty @Nullable String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty double bonusPoints,
        @JsonProperty @Nullable ZonedDateTime dueDate,
        @JsonProperty @Nullable ZonedDateTime startDate)
        implements ExerciseDTO {

    /**
     * Fetch all text exercises for a course.
     *
     * @param client   the artemis client to use
     * @param courseId the id of the course
     * @return a list of text exercises
     * @throws ArtemisNetworkException if the request fails
     */
    public static List<TextExerciseDTO> fetchAll(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        var exercises = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class);
        // Remove all non-text exercises
        return exercises.exercises().stream()
                .filter(e -> e instanceof TextExerciseDTO)
                .map(e -> (TextExerciseDTO) e)
                .toList();
    }

    /**
     * Artemis doesn't return the list of exercises directly, but only the list a
     * specific course with the exercises attached. We don't care about the course
     * here, so this wrapper class exists.
     */
    private record ExerciseWrapperDTO(List<ExerciseDTO> exercises) {
        private ExerciseWrapperDTO {
            if (exercises == null) {
                exercises = List.of();
            }
        }
    }
}
