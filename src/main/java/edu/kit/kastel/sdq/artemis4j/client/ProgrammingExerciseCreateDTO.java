/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * DTO for creating programming exercises.
 */
public record ProgrammingExerciseCreateDTO(
        @JsonProperty String type,
        @JsonProperty @Nullable EntityRefDTO course,
        @JsonProperty @Nullable EntityRefDTO exerciseGroup,
        @JsonProperty String title,
        @JsonProperty String shortName,
        @JsonProperty @Nullable String problemStatement,
        @JsonProperty @Nullable Set<String> categories,
        @JsonProperty @Nullable AssessmentType assessmentType,
        @JsonProperty @Nullable Double maxPoints,
        @JsonProperty @Nullable Double bonusPoints,
        @JsonProperty @Nullable Boolean allowComplaintsForAutomaticAssessments,
        @JsonProperty @Nullable Boolean allowFeedbackRequests,
        @JsonProperty @Nullable ZonedDateTime releaseDate,
        @JsonProperty @Nullable ZonedDateTime startDate,
        @JsonProperty @Nullable ZonedDateTime dueDate,
        @JsonProperty @Nullable ZonedDateTime assessmentDueDate,
        @JsonProperty @Nullable ZonedDateTime exampleSolutionPublicationDate,
        @JsonProperty @Nullable String testRepositoryUri,
        @JsonProperty @Nullable String solutionRepositoryUri,
        @JsonProperty @Nullable Boolean allowOnlineEditor,
        @JsonProperty @Nullable Boolean allowOfflineIde,
        @JsonProperty boolean allowOnlineIde,
        @JsonProperty @Nullable Boolean staticCodeAnalysisEnabled,
        @JsonProperty @Nullable Integer maxStaticCodeAnalysisPenalty,
        @JsonProperty @Nullable String programmingLanguage,
        @JsonProperty @Nullable String packageName,
        @JsonProperty boolean showTestNamesToStudents,
        @JsonProperty @Nullable ZonedDateTime buildAndTestStudentSubmissionsAfterDueDate,
        @JsonProperty @Nullable String projectType,
        @JsonProperty boolean releaseTestsWithExampleSolution,
        @JsonProperty ProgrammingExerciseBuildConfigCreateDTO buildConfig) {

    public ProgrammingExerciseCreateDTO forCourse(long targetCourseId) {
        return new ProgrammingExerciseCreateDTO(
                this.type,
                new EntityRefDTO(targetCourseId),
                null,
                title,
                shortName,
                problemStatement,
                categories,
                assessmentType,
                maxPoints,
                bonusPoints,
                allowComplaintsForAutomaticAssessments,
                allowFeedbackRequests,
                releaseDate,
                startDate,
                dueDate,
                assessmentDueDate,
                exampleSolutionPublicationDate,
                testRepositoryUri,
                solutionRepositoryUri,
                allowOnlineEditor,
                allowOfflineIde,
                allowOnlineIde,
                staticCodeAnalysisEnabled,
                maxStaticCodeAnalysisPenalty,
                programmingLanguage,
                packageName,
                showTestNamesToStudents,
                buildAndTestStudentSubmissionsAfterDueDate,
                projectType,
                releaseTestsWithExampleSolution,
                buildConfig);
    }

    public static ProgrammingExerciseCreateDTO minimalCourseExercise(
            String title, String shortName, String packageName, ProgrammingExerciseBuildConfigCreateDTO buildConfig) {
        return new ProgrammingExerciseCreateDTO(
                "programming",
                null,
                null,
                title,
                shortName,
                null,
                Set.of(),
                AssessmentType.SEMI_AUTOMATIC,
                10.0,
                0.0,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                true,
                false,
                false,
                null,
                "JAVA",
                packageName,
                false,
                null,
                "PLAIN_GRADLE",
                false,
                buildConfig);
    }

    public record EntityRefDTO(@JsonProperty long id) {}
}
