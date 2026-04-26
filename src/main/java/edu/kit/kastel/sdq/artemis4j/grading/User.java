/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import org.jspecify.annotations.Nullable;

/**
 * Represents a user in Artemis.
 */
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

    public @Nullable String getLangKey() {
        return this.dto.langKey();
    }

    /**
     * Get the first name of the user.
     * @return the first name
     */
    public @Nullable String getFirstName() {
        return this.dto.firstName();
    }

    /**
     * Get the last name of the user.
     * @return the last name
     */
    public @Nullable String getLastName() {
        return this.dto.lastName();
    }

    public Optional<String> getGitToken() {
        return Optional.ofNullable(this.dto.vcsAccessToken());
    }

    public Optional<ZonedDateTime> getGitTokenExpiryDate() {
        return Optional.ofNullable(this.dto.vcsAccessTokenExpiryDate());
    }

    public Optional<String> getVcsAccessToken() {
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
