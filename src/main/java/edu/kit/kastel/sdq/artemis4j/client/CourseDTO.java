/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record CourseDTO(
        @JsonProperty int id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty String instructorGroupName,
        @JsonProperty int numberOfInstructors,
        @JsonProperty int numberOfTeachingAssistants,
        @JsonProperty int numberOfEditors,
        @JsonProperty int numberOfStudents) {

    public static List<CourseDTO> fetchAll(ArtemisClient client) throws ArtemisNetworkException {
        var courses = ArtemisRequest.get()
                .path(List.of("courses", "with-user-stats"))
                .executeAndDecode(client, CourseDTO[].class);
        return Arrays.asList(courses);
    }

    public static List<UserDTO> fetchAllTutors(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        var tutors = ArtemisRequest.get()
                .path(List.of("courses", courseId, "tutors"))
                .executeAndDecode(client, UserDTO[].class);
        return Arrays.asList(tutors);
    }

    public static void removeTutor(ArtemisClient client, int courseId, String tutorLogin)
            throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("courses", courseId, "tutors", tutorLogin))
                .execute(client);
    }

    public static List<GenericSubmissionDTO> fetchLockedSubmissions(ArtemisClient client, int courseId)
            throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get()
                .path(List.of("courses", courseId, "lockedSubmissions"))
                .executeAndDecodeMaybe(client, GenericSubmissionDTO[].class)
                .orElse(new GenericSubmissionDTO[0]);
        return Arrays.asList(submissions);
    }
}
