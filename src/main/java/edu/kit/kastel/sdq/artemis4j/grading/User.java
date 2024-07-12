/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.UserDTO;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class User {
	private final UserDTO dto;

	public User(UserDTO dto) {
		this.dto = dto;
	}

	public long getId() {
		return this.dto.id();
	}

	public String getLogin() {
		return this.dto.login();
	}

	public String getLangKey() {
		return this.dto.langKey();
	}

	public Optional<String> getGitToken() {
		return Optional.ofNullable(this.dto.vcsAccessToken());
	}

	public List<String> getGroups() {
		return Collections.unmodifiableList(this.dto.groups());
	}

	protected UserDTO toDTO() {
		return this.dto;
	}

	@Override
	public String toString() {
		return this.getLogin();
	}
}
