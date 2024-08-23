/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record ProgrammingExerciseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty String testRepositoryUri,
        @JsonProperty Boolean secondCorrectionEnabled,
        @JsonProperty String exerciseType,
        @JsonProperty String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty ZonedDateTime dueDate,
        @JsonProperty ZonedDateTime startDate) {

    public static List<ProgrammingExerciseDTO> fetchAll(ArtemisClient client, int courseId)
            throws ArtemisNetworkException {
        var exercises = ArtemisRequest.get()
                .path(List.of("courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class);
        // Remove all non-programming exercises
        return Arrays.stream(exercises.exercises())
                .filter(e -> e.exerciseType().equals("PROGRAMMING"))
                .toList();
    }

    /**
     * Artemis doesn't return the list of exercises directly, but only the list a
     * specific course with the exercises attached. We don't care about the course
     * here, so this wrapper class exists.
     *
     * @param exercises
     */
    private record ExerciseWrapperDTO(ProgrammingExerciseDTO[] exercises) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ExerciseWrapperDTO that = (ExerciseWrapperDTO) o;
            return Arrays.equals(exercises, that.exercises);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(exercises);
        }
    }
}
