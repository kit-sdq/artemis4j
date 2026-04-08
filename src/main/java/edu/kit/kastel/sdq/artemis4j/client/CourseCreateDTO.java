/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * DTO for creating a new course via the Artemis admin API.
 */
public record CourseCreateDTO(
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty @Nullable String description,
        @JsonProperty @Nullable String semester,
        @JsonProperty @Nullable String studentGroupName,
        @JsonProperty @Nullable String teachingAssistantGroupName,
        @JsonProperty @Nullable String editorGroupName,
        @JsonProperty @Nullable String instructorGroupName,
        @JsonProperty @Nullable String startDate,
        @JsonProperty @Nullable String endDate,
        @JsonProperty @Nullable String enrollmentStartDate,
        @JsonProperty @Nullable String enrollmentEndDate,
        @JsonProperty @Nullable String unenrollmentEndDate,
        @JsonProperty boolean testCourse,
        @JsonProperty @Nullable Boolean onlineCourse,
        @JsonProperty @Nullable String language,
        @JsonProperty @Nullable String defaultProgrammingLanguage,
        @JsonProperty @Nullable Integer maxComplaints,
        @JsonProperty @Nullable Integer maxTeamComplaints,
        @JsonProperty int maxComplaintTimeDays,
        @JsonProperty int maxRequestMoreFeedbackTimeDays,
        @JsonProperty int maxComplaintTextLimit,
        @JsonProperty int maxComplaintResponseTextLimit,
        @JsonProperty @Nullable String color,
        @JsonProperty @Nullable Boolean enrollmentEnabled,
        @JsonProperty @Nullable String enrollmentConfirmationMessage,
        @JsonProperty boolean unenrollmentEnabled,
        @JsonProperty boolean learningPathsEnabled,
        @JsonProperty boolean studentCourseAnalyticsDashboardEnabled,
        @JsonProperty @Nullable Integer presentationScore,
        @JsonProperty @Nullable Integer maxPoints,
        @JsonProperty @Nullable Integer accuracyOfScores,
        @JsonProperty boolean restrictedAthenaModulesAccess,
        @JsonProperty @Nullable String timeZone,
        @JsonProperty @Nullable String courseInformationSharingConfiguration) {

    /**
     * Creates a minimal course-creation DTO with sensible defaults.
     */
    public static CourseCreateDTO minimal(String title, String shortName) {
        return new CourseCreateDTO(
                title, shortName, null, null, null, null, null, null, null, null, null, null, null, false, null, null,
                null, null, null, 7, 7, 2000, 2000, null, null, null, false, false, false, null, null, null, false,
                null, null);
    }
}
