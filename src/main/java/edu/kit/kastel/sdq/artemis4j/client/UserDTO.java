/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record UserDTO(
        @JsonProperty long id,
        @JsonProperty String login,
        @JsonProperty String firstName,
        @JsonProperty String lastName,
        @JsonProperty String email,
        @JsonProperty boolean activated,
        @JsonProperty String langKey,
        @JsonProperty String lastNotificationRead,
        @JsonProperty String name,
        @JsonProperty String participantIdentifier,
        @JsonProperty List<String> groups,
        @JsonProperty String vcsAccessToken,
        @JsonProperty ZonedDateTime vcsAccessTokenExpiryDate,
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
        return ArtemisRequest.get().path(List.of("core", "public", "account")).executeAndDecode(client, UserDTO.class);
    }

    public static void createVCSToken(ZonedDateTime expiryDate, ArtemisClient client) throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("core", "account", "user-vcs-access-token"))
                .param("expiryDate", expiryDate)
                .execute(client);
    }

    public static List<String> getUnenrolledUsers(ArtemisClient client) throws ArtemisNetworkException {
        var unenrolledUsers = ArtemisRequest.get()
                .path(List.of("core", "admin", "users", "not-enrolled"))
                .executeAndDecodeMaybe(client, String[].class)
                .orElseThrow();
        return List.of(unenrolledUsers);
    }

    public static void deleteUser(ArtemisClient client, String username) throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("core", "admin", "users"))
                .param("login", username)
                .execute(client);
    }
}
