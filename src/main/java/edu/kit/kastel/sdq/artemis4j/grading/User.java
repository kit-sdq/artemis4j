package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.UserDTO;

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

    protected UserDTO toDTO() {
        return this.dto;
    }
}