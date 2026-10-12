/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal data that is common to all possible submission DTOs.
 */
public record GenericSubmissionDTO(
        @JsonProperty String submissionExerciseType,
        @JsonProperty long id) {}
