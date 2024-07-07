/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthenticationDTO(@JsonProperty String username, @JsonProperty String password, @JsonProperty boolean rememberMe) {
	public AuthenticationDTO(String username, String password) {
		this(username, password, true);
	}
}
