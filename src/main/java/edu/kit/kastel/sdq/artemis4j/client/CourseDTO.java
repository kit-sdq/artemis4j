/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jspecify.annotations.Nullable;

public record CourseDTO(
        @JsonProperty long id,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty @Nullable String studentGroupName,
        @JsonProperty @Nullable String teachingAssistantGroupName,
        @JsonProperty @Nullable String editorGroupName,
        @JsonProperty @Nullable String instructorGroupName,
        @JsonProperty @Nullable Integer numberOfInstructors,
        @JsonProperty @Nullable Integer numberOfTeachingAssistants,
        @JsonProperty @Nullable Integer numberOfEditors,
        @JsonProperty @Nullable Integer numberOfStudents,
        @JsonProperty @Nullable List<ExerciseDTO> exercises) {
    private Map<String, CourseRole> getRoleMapping() {
        Map<String, CourseRole> result = new HashMap<>();

        if (this.studentGroupName != null) {
            result.put(this.studentGroupName, CourseRole.STUDENT);
        }

        if (this.teachingAssistantGroupName != null) {
            result.put(this.teachingAssistantGroupName, CourseRole.TUTOR);
        }

        if (this.editorGroupName != null) {
            result.put(this.editorGroupName, CourseRole.EDITOR);
        }

        if (this.instructorGroupName != null) {
            result.put(this.instructorGroupName, CourseRole.INSTRUCTOR);
        }

        return result;
    }

    public Set<CourseRole> mapToCourseRoles(Collection<String> groupNames) {
        var mapping = this.getRoleMapping();

        return groupNames.stream().map(mapping::get).collect(Collectors.toSet());
    }

    public Set<String> mapToGroupNames(Collection<CourseRole> courseRoles) {
        var mapping = this.getRoleMapping();
        var reverseMapping =
                mapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

        return courseRoles.stream().map(reverseMapping::get).collect(Collectors.toSet());
    }

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
                .executeAndDecode(client, CoursesForDashboardDTO.class);
        return courses.courses().stream().map(CourseForDashboardDTO::course).toList();
    }

    private record CoursesForDashboardDTO(
            @JsonProperty List<CourseForDashboardDTO> courses,
            @JsonProperty List<ExamDTO> activeExams) {}

    private record CourseForDashboardDTO(@JsonProperty CourseDTO course) {}

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

    public static List<UserPublicInfoDTO> searchUsers(
            ArtemisClient client, long courseId, String loginOrName, Iterable<String> roles)
            throws ArtemisNetworkException {
        var users = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "users", "search"))
                .param("loginOrName", loginOrName)
                .param("roles", String.join(",", roles))
                .executeAndDecodeMaybe(client, UserPublicInfoDTO[].class)
                .orElse(new UserPublicInfoDTO[0]);
        return Arrays.asList(users);
    }

    public static void removeTutor(ArtemisClient client, long courseId, String tutorLogin)
            throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("core", "courses", courseId, "tutors", tutorLogin))
                .execute(client);
    }

    public static List<GenericSubmissionDTO> fetchLockedSubmissions(ArtemisClient client, long courseId)
            throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get()
                .path(List.of("core", "courses", courseId, "locked-submissions"))
                .executeAndDecodeMaybe(client, GenericSubmissionDTO[].class)
                .orElse(new GenericSubmissionDTO[0]);
        return Arrays.asList(submissions);
    }
}
