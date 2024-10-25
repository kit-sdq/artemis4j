/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AssessmentStatsDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextSubmissionDTO;

/**
 * A text exercise in artemis.
 */
public class TextExercise extends ArtemisConnectionHolder implements Exercise {
    private final TextExerciseDTO dto;

    private final Course course;

    /**
     * Creates a new text exercise.
     *
     * @param dto the DTO for this exercise
     * @param course the course this exercise is part of
     */
    public TextExercise(TextExerciseDTO dto, Course course) {
        super(course);

        this.dto = dto;
        this.course = course;
    }

    @Override
    public long getId() {
        return this.dto.id();
    }

    @Override
    public String getTitle() {
        return this.dto.title();
    }

    @Override
    public String getShortName() {
        return this.dto.shortName();
    }

    @Override
    public Course getCourse() {
        return this.course;
    }

    @Override
    public AssessmentStatsDTO fetchAssessmentStats() throws ArtemisNetworkException {
        // TODO Implement this
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Returns the type of assessment for this exercise.
     *
     * @return the assessment type, likely "manual" or "automatic"
     */
    public String getAssessmentType() {
        return this.dto.assessmentType();
    }

    /**
     * Returns the maximum points one can achieve in this exercise.
     *
     * @return the maximum points
     */
    public double getMaxPoints() {
        return this.dto.maxPoints();
    }

    /**
     * Returns the bonus points for this exercise.
     *
     * @return the bonus points or 0.0 if there are none
     */
    public double getBonusPoints() {
        return this.dto.bonusPoints();
    }

    /**
     * When the exercise is due.
     *
     * @return the due date
     */
    public ZonedDateTime getDueDate() {
        return this.dto.dueDate();
    }

    /**
     * When the exercise starts.
     *
     * @return the start date
     */
    public ZonedDateTime getStartDate() {
        return this.dto.startDate();
    }

    /**
     * Fetches all submissions for this exercise. This may fetch *many* submissions,
     * and does not cache the result, so be careful.
     *
     * @param correctionRound       The correction round to fetch submissions for
     * @param filterAssessedByTutor Whether to only fetch submissions that the
     *                              current user has assessed
     * @return a list of submissions
     * @throws ArtemisNetworkException if the request fails
     */
    public List<TextSubmission> fetchSubmissions(int correctionRound, boolean filterAssessedByTutor)
            throws ArtemisNetworkException {
        return TextSubmissionDTO.fetchAll(
                        this.getConnection().getClient(), this.getId(), correctionRound, filterAssessedByTutor)
                .stream()
                .map(submissionDto -> new TextSubmission(submissionDto, this, correctionRound))
                .toList();
    }

    /**
     * Tries to lock the next submission for this exercise. If successful, returns
     * the assessment.
     *
     * @param correctionRound the correction round to lock the submission for
     *
     * @return An empty optional if no submission was available to lock, otherwise
     *         the assessment
     */
    public Optional<TextAssessment> tryLockNextSubmission(int correctionRound) throws ArtemisNetworkException {

        // This line already locks the submission, but doesn't tell us what the relevant
        // ResultDTO is
        var nextSubmissionDto =
                TextSubmissionDTO.lockNextSubmission(this.getConnection().getClient(), this.getId(), correctionRound);
        if (nextSubmissionDto.isEmpty()) {
            return Optional.empty();
        }

        var locked = nextSubmissionDto.get();

        if (locked.results() == null) {
            throw new IllegalStateException("Locking returned a submission %d without results".formatted(locked.id()));
        }

        if (locked.results().size() != 1) {
            throw new IllegalStateException("Locking returned %d results, expected 1"
                    .formatted(locked.results().size()));
        }
        var result = locked.results().get(0);

        if (this.canAssess(result)) {
            return Optional.empty();
        }

        var submission = new TextSubmission(locked, this, correctionRound);
        return Optional.of(new TextAssessment(result, submission, correctionRound));
    }

    private boolean canAssess(ResultDTO result) throws ArtemisNetworkException {
        // We can assess if either no assessor is set, we are the assessor,
        // or if we are an instructor (who can overwrite any assessment)
        var assessor = this.getConnection().getAssessor();
        return result.assessor() == null
                || result.assessor().id() != assessor.getId()
                || this.getCourse().isInstructor(assessor);
    }
}
