package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty String participantIdentifier,
        @JsonProperty String userIndependentRepositoryUri,
        @JsonProperty ResultDTO[] results
) {
}
