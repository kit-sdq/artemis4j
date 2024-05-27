package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;

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
}
