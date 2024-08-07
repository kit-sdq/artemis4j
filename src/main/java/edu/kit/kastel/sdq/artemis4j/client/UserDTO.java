/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record UserDTO(@JsonProperty long id, @JsonProperty String login, @JsonProperty String firstName, @JsonProperty String lastName,
        @JsonProperty String email, @JsonProperty boolean activated, @JsonProperty String langKey, @JsonProperty String lastNotificationRead,
        @JsonProperty String name, @JsonProperty String participantIdentifier, @JsonProperty List<String> groups, @JsonProperty String vcsAccessToken,
        @JsonProperty String sshPublicKey) {
    public UserDTO {
        if (groups == null) {
            // when a user is not in any group, artemis returns null
            groups = List.of();
        } else {
            groups = List.copyOf(groups);
        }
    }

    public static UserDTO getAssessingUser(ArtemisClient client) throws ArtemisNetworkException {
        return ArtemisRequest.get().path(List.of("public", "account")).executeAndDecode(client, UserDTO.class);
    }
}
