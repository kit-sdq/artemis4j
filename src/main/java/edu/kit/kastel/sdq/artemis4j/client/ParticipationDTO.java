/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty @Nullable UserDTO student,
        @JsonProperty String participantIdentifier,
        @JsonProperty String userIndependentRepositoryUri,
        @JsonProperty List<ResultDTO> results) {}
