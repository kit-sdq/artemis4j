/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Build configuration DTO for programming exercise creation.
 */
public record ProgrammingExerciseBuildConfigCreateDTO(
        @JsonProperty @Nullable Boolean sequentialTestRuns,
        @JsonProperty @Nullable String buildPlanConfiguration,
        @JsonProperty @Nullable String buildScript,
        @JsonProperty boolean checkoutSolutionRepository,
        @JsonProperty @Nullable String testCheckoutPath,
        @JsonProperty @Nullable String assignmentCheckoutPath,
        @JsonProperty @Nullable String solutionCheckoutPath,
        @JsonProperty int timeoutSeconds,
        @JsonProperty @Nullable String dockerFlags,
        @JsonProperty boolean allowBranching,
        @JsonProperty @Nullable String branchRegex) {

    public static ProgrammingExerciseBuildConfigCreateDTO defaults() {
        return new ProgrammingExerciseBuildConfigCreateDTO(
                null, null, null, false, null, null, null, 120, null, false, ".*");
    }
}
