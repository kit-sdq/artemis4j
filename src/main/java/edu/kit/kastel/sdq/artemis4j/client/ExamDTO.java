/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record ExamDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty int numberOfCorrectionRoundsInExam,
        @JsonProperty ZonedDateTime startDate,
        @JsonProperty ZonedDateTime endDate,
        @JsonProperty List<ExerciseGroupDTO> exerciseGroups) {

    /**
     * This call does not populate exerciseGroups - they will be null! Use
     * {@link #fetch(ArtemisClient client, int courseId, long examId)} to get the
     * full exam.
     */
    public static List<ExamDTO> fetchAll(ArtemisClient client, int courseId) throws ArtemisNetworkException {
        return List.of(ArtemisRequest.get()
                .path(List.of("courses", courseId, "exams"))
                .executeAndDecode(client, ExamDTO[].class));
    }

    public static ExamDTO fetch(ArtemisClient client, int courseId, long examId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("courses", courseId, "exams", examId, "exam-for-assessment-dashboard"))
                .executeAndDecode(client, ExamDTO.class);
    }
}
