package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.dto.artemis.UserDTO;

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

    protected UserDTO toDTO() {
        return this.dto;
    }
}
