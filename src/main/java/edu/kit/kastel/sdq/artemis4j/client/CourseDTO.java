/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jspecify.annotations.Nullable;

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
                .path(List.of("core", "courses", "with-user-stats"))
                .executeAndDecode(client, CourseDTO[].class);
        return Arrays.asList(courses);
    }

    public static List<CourseDTO> fetchForEnrollment(ArtemisClient client) throws ArtemisNetworkException {
        var courses = ArtemisRequest.get()
                .path(List.of("core", "courses", "for-enrollment"))
                .executeAndDecode(client, CourseDTO[].class);
        return Arrays.asList(courses);
    }

    public static List<CourseDTO> fetchForDashboard(ArtemisClient client) throws ArtemisNetworkException {
        var courses = ArtemisRequest.get()
                .path(List.of("core", "courses", "for-dashboard"))
                .executeAndDecode(client, CourseDTO[].class);
        return Arrays.asList(courses);
    }

    public static @Nullable CourseDTO createCourse(ArtemisClient client, CourseCreateDTO courseCreateDTO)
            throws ArtemisNetworkException {
        return createCourse(client, courseCreateDTO, null, null, null);
    }

    public static @Nullable CourseDTO createCourse(
            ArtemisClient client,
            CourseCreateDTO courseCreateDTO,
            byte @Nullable [] courseIcon,
            @Nullable String filename,
            @Nullable String mediaType)
            throws ArtemisNetworkException {
        RequestBody courseBody = ArtemisClient.encodeJSON(courseCreateDTO);
        var multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("course", "course.json", courseBody);

        if (courseIcon != null && courseIcon.length > 0) {
            var iconType = mediaType != null ? mediaType : "application/octet-stream";
            var iconName = filename != null && !filename.isBlank() ? filename : "course-icon.bin";
            multipartBuilder.addFormDataPart(
                    "file", iconName, RequestBody.create(courseIcon, okhttp3.MediaType.get(iconType)));
        }

        var request = new Request.Builder()
                .url(client.getInstance().url(List.of("core", "admin", "courses"), null))
                .post(multipartBuilder.build())
                .build();
        return client.call(request, CourseDTO.class);
    }

    public static void deleteCourse(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("core", "admin", "courses", courseId))
                .execute(client);
    }

    public static void enrollInCourse(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        ArtemisRequest.post()
                .path(List.of("core", "courses", courseId, "enroll"))
                .execute(client);
    }

    public static void assignUserToCourse(ArtemisClient client, long courseId, String userLogin, CourseRole role)
            throws ArtemisNetworkException {
        ArtemisRequest.post()
                .path(List.of("core", "courses", courseId, role.toString(), userLogin))
                .execute(client);
    }

    public static List<UserDTO> fetchAllTutors(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        var tutors = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "tutors"))
                .executeAndDecode(client, UserDTO[].class);
        return Arrays.asList(tutors);
    }

    public static List<UserDTO> fetchAllStudents(ArtemisClient client, long courseId) throws ArtemisNetworkException {
        var students = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "students"))
                .executeAndDecode(client, UserDTO[].class);
        return Arrays.asList(students);
    }

    public static void removeTutor(ArtemisClient client, int courseId, String tutorLogin)
            throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("core", "courses", courseId, "tutors", tutorLogin))
                .execute(client);
    }

    public static List<GenericSubmissionDTO> fetchLockedSubmissions(ArtemisClient client, int courseId)
            throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "locked-submissions"))
                .executeAndDecodeMaybe(client, GenericSubmissionDTO[].class)
                .orElse(new GenericSubmissionDTO[0]);
        return Arrays.asList(submissions);
    }
}
