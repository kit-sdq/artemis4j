/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record ExamDTO(
        @JsonProperty long id,
        @JsonProperty @Nullable String title,
        @JsonProperty int numberOfCorrectionRoundsInExam,
        @JsonProperty @Nullable ZonedDateTime startDate,
        @JsonProperty @Nullable ZonedDateTime endDate,
        @JsonProperty @Nullable List<ExerciseGroupDTO> exerciseGroups) {

    public boolean hasSecondCorrectionRound() {
        return this.numberOfCorrectionRoundsInExam > 1;
    }

    public static List<ExamDTO> fetchAll(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        return List.of(ArtemisRequest.get()
                .path(List.of("exam", "courses", courseId, "exams"))
                .executeAndDecode(client, ExamDTO[].class));
    }
}
