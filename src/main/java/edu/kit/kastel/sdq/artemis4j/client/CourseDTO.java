package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

import java.util.Arrays;
import java.util.List;

public record CourseDTO(
        @JsonProperty int id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty String instructorGroup,
        @JsonProperty int numberOfInstructors,
        @JsonProperty int numberOfTeachingAssistants,
        @JsonProperty int numberOfEditors,
        @JsonProperty int numberOfStudents
) {

    public static List<CourseDTO> fetchAll(ArtemisClient client) throws ArtemisNetworkException {
        var courses = ArtemisRequest.get()
                .path(List.of("courses", "with-user-stats"))
                .executeAndDecode(client, CourseDTO[].class);
        return Arrays.asList(courses);
    }
}
