/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ParticipationDTO;

/**
 * A participation in an exercise.
 */
public class Participation extends ArtemisConnectionHolder {
    private final ParticipationDTO dto;

    public Participation(ParticipationDTO dto, ArtemisConnectionHolder connectionHolder) {
        this(dto, connectionHolder.getConnection());
    }

    public Participation(ParticipationDTO dto, ArtemisConnection connection) {
        super(connection);
        this.dto = dto;
    }

    public long getId() {
        return this.dto.id();
    }

    public String getParticipantIdentifier() {
        return this.dto.participantIdentifier();
    }

    public Optional<String> getRepositoryUri() {
        return Optional.ofNullable(this.dto.repositoryUri());
    }

    public Optional<String> getRepositoryUrl() {
        return this.dto.repositoryUrl();
    }

    /**
     * The student can only be retrieved by instructors.
     */
    public Optional<User> getStudent() {
        return Optional.ofNullable(this.dto.student()).map(User::new);
    }

    public String getVcsAccessToken() throws ArtemisNetworkException {
        return ParticipationDTO.getVcsAccessToken(this.getConnection().getClient(), this.getId());
    }

    public ParticipationDTO getDTO() {
        return this.dto;
    }

    @Override
    public String toString() {
        return this.getParticipantIdentifier();
    }
}
