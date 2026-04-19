/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;

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
        @JsonProperty @Nullable AssessmentType assessmentType,
        @JsonProperty @Nullable Double maxPoints,
        @JsonProperty @Nullable Double bonusPoints,
        @JsonProperty @Nullable ZonedDateTime releaseDate,
        @JsonProperty @Nullable ZonedDateTime startDate,
        @JsonProperty @Nullable ZonedDateTime dueDate,
        @JsonProperty @Nullable ZonedDateTime assessmentDueDate,
        @JsonProperty @Nullable String testRepositoryUri,
        @JsonProperty @Nullable String solutionRepositoryUri,
        @JsonProperty @Nullable Boolean allowOfflineIde,
        @JsonProperty @Nullable Boolean staticCodeAnalysisEnabled,
        @JsonProperty @Nullable Integer maxStaticCodeAnalysisPenalty,
        @JsonProperty @Nullable String packageName,
        @JsonProperty boolean showTestNamesToStudents,
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
                assessmentType,
                maxPoints,
                bonusPoints,
                releaseDate,
                startDate,
                dueDate,
                assessmentDueDate,
                testRepositoryUri,
                solutionRepositoryUri,
                allowOfflineIde,
                staticCodeAnalysisEnabled,
                maxStaticCodeAnalysisPenalty,
                packageName,
                showTestNamesToStudents,
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
                AssessmentType.SEMI_AUTOMATIC,
                10.0,
                0.0,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                false,
                null,
                packageName,
                false,
                "PLAIN_MAVEN",
                false,
                buildConfig);
    }

    public record EntityRefDTO(@JsonProperty long id) {}
}
