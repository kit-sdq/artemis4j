package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty UserDTO student,
        @JsonProperty String participantIdentifier,
        @JsonProperty String userIndependentRepositoryUri,
        @JsonProperty List<ResultDTO> results
) {
}
