/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record UserSshPublicKeyDTO(
        @JsonProperty Long id,
        @JsonProperty String label,
        @JsonProperty String publicKey,
        @JsonProperty String keyHash,
        @JsonProperty ZonedDateTime creationDate,
        @JsonProperty @Nullable ZonedDateTime lastUsedDate,
        @JsonProperty @Nullable ZonedDateTime expiryDate) {

    public static List<UserSshPublicKeyDTO> getCurrentUserSshKeys(ArtemisClient client) throws ArtemisNetworkException {
        return Arrays.asList(ArtemisRequest.get()
                .path(List.of("programming", "ssh-settings", "public-keys"))
                .executeAndDecode(client, UserSshPublicKeyDTO[].class));
    }
}
