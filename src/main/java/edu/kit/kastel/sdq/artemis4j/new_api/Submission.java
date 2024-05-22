package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.new_client.AnnotationMappingException;

import java.util.Optional;

public class Submission extends ArtemisClientHolder {
    private final SubmissionDTO dto;

    private final Exercise exercise;

    public Submission(SubmissionDTO dto, Exercise exercise) {
        super(exercise);

        this.dto = dto;
        this.exercise = exercise;
    }

    public long getId() {
        return this.dto.id();
    }

    public long getParticipationId() {
        return this.dto.participation().id();
    }

    public String getRepositoryUrl() {
        return this.dto.participation().userIndependentRepositoryUri();
    }

    public String getCommitHash() {
        return this.dto.commitHash();
    }

    public boolean hasBuildFailed() {
        return this.dto.buildFailed();
    }

    public Exercise getExercise() {
        return exercise;
    }

    public Optional<Assessment> tryLock(int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException {
        var locked = SubmissionDTO.lock(this.getClient(), this.getId(), correctionRound);

        if (locked.id() != this.getId()) {
            throw new IllegalStateException("Locking returned a different submission than requested??");
        }

        // We should have exactly one result because we specified a correction round
        if (locked.results().length != 1) {
            throw new IllegalStateException("Locking returned %d results, expected 1".formatted(locked.results().length));
        }
        var result = locked.results()[0];

        // Locking was successful if we are the assessor
        // The webui of Artemis does the same check
        if (result.assessor() == null || result.assessor().id() != this.getClient().getAssessor().getId()) {
            return Optional.empty();
        }

        return Optional.of(new Assessment(result, gradingConfig, this));
    }
}
