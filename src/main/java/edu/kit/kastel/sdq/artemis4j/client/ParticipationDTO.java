/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty @Nullable UserDTO student,
        @JsonProperty String participantIdentifier,
        @JsonProperty @Nullable String userIndependentRepositoryUri,
        @JsonProperty @Nullable String repositoryUri,
        @JsonProperty List<ResultDTO> results) {

    public static ParticipationDTO startExercise(ArtemisClient client, long exerciseId) throws ArtemisNetworkException {
        return ArtemisRequest.post()
                .path(List.of("exercise", "exercises", exerciseId, "participations"))
                .executeAndDecode(client, ParticipationDTO.class);
    }

    public Optional<String> repositoryUrl() {
        if (userIndependentRepositoryUri != null && !userIndependentRepositoryUri.isBlank()) {
            return Optional.of(userIndependentRepositoryUri);
        }
        if (repositoryUri != null && !repositoryUri.isBlank()) {
            return Optional.of(repositoryUri);
        }
        return Optional.empty();
    }
}
