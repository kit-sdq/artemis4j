/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.time.ZonedDateTime;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.UserSshPublicKeyDTO;

public class UserSshPublicKey extends ArtemisConnectionHolder {
    private final UserSshPublicKeyDTO dto;

    public UserSshPublicKey(UserSshPublicKeyDTO dto, ArtemisConnection connection) {
        super(connection);

        this.dto = dto;
    }

    public long getId() {
        return this.dto.id();
    }

    public String getPublicKey() {
        return this.dto.publicKey();
    }

    public String getHash() {
        return this.dto.keyHash();
    }

    public ZonedDateTime getCreationDate() {
        return this.dto.creationDate();
    }

    public Optional<ZonedDateTime> getExpirationDate() {
        return Optional.ofNullable(this.dto.expiryDate());
    }

    public Optional<ZonedDateTime> getLastUsedDate() {
        return Optional.ofNullable(this.dto.lastUsedDate());
    }
}
