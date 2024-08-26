/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty UserDTO student,
        @JsonProperty String participantIdentifier,
        @JsonProperty String userIndependentRepositoryUri,
        @JsonProperty List<ResultDTO> results) {}
