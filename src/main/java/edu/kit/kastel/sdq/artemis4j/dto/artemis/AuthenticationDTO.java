package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationDTO(
        @JsonProperty String username,
        @JsonProperty String password,
        @JsonProperty boolean rememberMe
) {
    public AuthenticationDTO(String username, String password) {
        this(username, password, true);
    }
}
