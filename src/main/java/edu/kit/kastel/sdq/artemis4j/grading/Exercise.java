package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.InvalidGradingConfigException;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.SubmissionDTO;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * An exercise, containing many submissions.
 */
public class Exercise extends ArtemisConnectionHolder {
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

    /**
     * Fetches all submissions for this exercise. This may fetch *many* submissions, and does not catch the result, so be careful.
     * @param correctionRound The correction round to fetch submissions for
     * @param onlyOwn Whether to only fetch submissions that the current user has assessed
     * @return
     * @throws ArtemisNetworkException
     */
    public List<Submission> fetchSubmissions(int correctionRound, boolean onlyOwn) throws ArtemisNetworkException {
        return SubmissionDTO.fetchAll(this.getConnection().getClient(), this.getId(), correctionRound, onlyOwn)
                .stream()
                .map(dto -> new Submission(dto, this))
                .toList();
    }

    /**
     * Tries to lock the next submission for this exercise. If successful, returns the assessment.
     * @param correctionRound
     * @param gradingConfig
     * @return An empty optional if no submission was available to lock, otherwise the assessment
     * @throws AnnotationMappingException
     * @throws ArtemisNetworkException
     * @throws InvalidGradingConfigException
     */
    public Optional<Assessment> tryLockNextSubmission(int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException, InvalidGradingConfigException {
        this.assertGradingConfigValid(gradingConfig);

        // This line already locks the submission, but doesn't tell us what the relevant ResultDTO is
        var dto = SubmissionDTO.lockNextSubmission(this.getConnection().getClient(), this.getId(), correctionRound);
        if (dto.isEmpty()) {
            return Optional.empty();
        }

        // Second lock call to get the ResultDTO
        var lockResult = this.tryLockSubmission(dto.get().id(), correctionRound, gradingConfig);
        return Optional.of(lockResult.orElseThrow(IllegalStateException::new));
    }

    /**
     * Tries to lock a specific submission for this exercise. Locking is reentrant, i.e. a single user may lock the same submission multiple times.
     * @param submissionId
     * @param correctionRound
     * @param gradingConfig
     * @return An empty optional if a *different* user has already locked the submission, otherwise the assessment
     * @throws AnnotationMappingException
     * @throws ArtemisNetworkException
     * @throws InvalidGradingConfigException
     */
    public Optional<Assessment> tryLockSubmission(long submissionId, int correctionRound, GradingConfig gradingConfig) throws AnnotationMappingException, ArtemisNetworkException, InvalidGradingConfigException {
        this.assertGradingConfigValid(gradingConfig);

        var locked = SubmissionDTO.lock(this.getConnection().getClient(), submissionId, correctionRound);

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
        if (result.assessor() == null || result.assessor().id() != this.getConnection().getAssessor().getId()) {
            return Optional.empty();
        }

        var submission = new Submission(locked, this);
        return Optional.of(new Assessment(result, gradingConfig, submission, correctionRound));
    }

    private void assertGradingConfigValid(GradingConfig gradingConfig) {
        if (!gradingConfig.isValidForExercise(this)) {
            throw new IllegalArgumentException("Grading config is not valid for this exercise");
        }
    }
}
