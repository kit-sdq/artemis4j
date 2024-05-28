package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.new_api.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.new_api.penalty.InvalidGradingConfigException;
import edu.kit.kastel.sdq.artemis4j.new_client.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.SubmissionDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public class Exercise extends ArtemisClientHolder {
    private final ExerciseDTO dto;

    private final Course course;

    public Exercise(ExerciseDTO dto, Course course) {
        super(course);

        this.dto = dto;
        this.course = course;
    }

    public long getId() {
        return this.dto.id();
    }

    public String getTestRepositoryUrl() {
        return this.dto.testRepositoryUri();
    }

    public double getMaxPoints() {
        return this.dto.maxPoints();
    }

    public ZonedDateTime getStartDate() {
        return this.dto.startDate();
    }

    public ZonedDateTime getDueDate() {
        return this.dto.dueDate();
    }

    public String getTitle() {
        return this.dto.title();
    }

    public String getShortName() {
        return this.dto.shortName();
    }

    public Course getCourse() {
        return this.course;
    }

    public List<Submission> fetchSubmissions(int correctionRound, boolean onlyOwn) throws ArtemisNetworkException {
        return SubmissionDTO.fetchAll(this.getClient(), this.getId(), correctionRound, onlyOwn)
                .stream()
                .map(dto -> new Submission(dto, this))
                .toList();
    }

    public Optional<Assessment> tryLockNextSubmission(int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException, InvalidGradingConfigException {
        this.assertGradingConfigValid(gradingConfig);

        // This line already locks the submission, but doesn't tell us what the relevant ResultDTO is
        var dto = SubmissionDTO.lockNextSubmission(this.getClient(), this.getId(), correctionRound);
        if (dto.isEmpty()) {
            return Optional.empty();
        }

        // Second lock call to get the ResultDTO
        var lockResult = this.tryLockSubmission(dto.get().id(), correctionRound, gradingConfig);
        return Optional.of(lockResult.orElseThrow(IllegalStateException::new));
    }

    public Optional<Assessment> tryLockSubmission(long submissionId, int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException, InvalidGradingConfigException {
        this.assertGradingConfigValid(gradingConfig);

        var locked = SubmissionDTO.lock(this.getClient(), submissionId, correctionRound);

        if (locked.id() != submissionId) {
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

        var submission = new Submission(locked, this);
        return Optional.of(new Assessment(result, gradingConfig, submission));
    }

    private void assertGradingConfigValid(GradingConfig gradingConfig) {
        if (!gradingConfig.isValidForExercise(this)) {
            throw new IllegalArgumentException("Grading config is not valid for this exercise");
        }
    }
}
