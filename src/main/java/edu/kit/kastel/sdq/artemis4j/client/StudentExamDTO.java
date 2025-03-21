/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record StudentExamDTO(@JsonProperty long id, @JsonProperty boolean submitted, @JsonProperty UserDTO user) {
    public static List<StudentExamDTO> fetchAll(ArtemisClient client, int courseId, long examId)
            throws ArtemisNetworkException {
        return Arrays.asList(ArtemisRequest.get()
                .path(List.of("exam", "courses", courseId, "exams", examId, "student-exams"))
                .executeAndDecode(client, StudentExamDTO[].class));
    }

    public static void toggleToSubmitted(ArtemisClient client, int courseId, long examId, long studentExamId)
            throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of(
                        "exam",
                        "courses",
                        courseId,
                        "exams",
                        examId,
                        "student-exams",
                        studentExamId,
                        "toggle-to-submitted"))
                .body("{}")
                .execute(client);
    }
}
