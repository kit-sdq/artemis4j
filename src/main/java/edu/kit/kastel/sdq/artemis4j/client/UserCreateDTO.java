/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * DTO for creating a new user in Artemis
 */
public record UserCreateDTO(
        @JsonProperty String login,
        @JsonProperty String firstName,
        @JsonProperty String lastName,
        @JsonProperty String email,
        @JsonProperty @Nullable String password,
        @JsonProperty @Nullable String langKey) {}
