package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisRequest;

import java.util.List;

public record UserDTO(
        @JsonProperty long id,
        @JsonProperty String login,
        @JsonProperty String firstName,
        @JsonProperty String lastName,
        @JsonProperty String email,
        @JsonProperty String langKey,
        @JsonProperty String vcsAccessToken
) {

    public static UserDTO getAssessingUser(ArtemisClient client) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("public", "account"))
                .executeAndDecode(client, UserDTO.class);
    }
}
