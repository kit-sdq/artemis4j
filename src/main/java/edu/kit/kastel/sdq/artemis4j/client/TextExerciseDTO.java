/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record TextExerciseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty String exerciseType,
        @JsonProperty String assessmentType,
        @JsonProperty double maxPoints,
        @JsonProperty double bonusPoints,
        @JsonProperty ZonedDateTime dueDate,
        @JsonProperty ZonedDateTime startDate,
        @JsonProperty boolean automaticAssessmentEnabled) {

    /**
     * Fetch all text exercises for a course.
     *
     * @param client   the artemis client to use
     * @param courseId the id of the course
     * @return a list of text exercises
     * @throws ArtemisNetworkException if the request fails
     */
    public static List<TextExerciseDTO> fetchAll(ArtemisClient client, int courseId) throws ArtemisNetworkException {
        var exercises = ArtemisRequest.get()
                .path(List.of("courses", courseId, "with-exercises"))
                .executeAndDecode(client, ExerciseWrapperDTO.class);
        // Remove all non-text exercises
        return exercises.exercises().stream()
                .filter(e -> e.exerciseType().equals("TEXT"))
                .toList();
    }

    /**
     * Artemis doesn't return the list of exercises directly, but only the list a
     * specific course with the exercises attached. We don't care about the course
     * here, so this wrapper class exists.
     */
    private record ExerciseWrapperDTO(List<TextExerciseDTO> exercises) {}
}
