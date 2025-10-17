/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.AssessmentStatsDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.grading.metajson.AnnotationMappingException;
import edu.kit.kastel.sdq.artemis4j.grading.penalty.GradingConfig;

/**
 * An exercise, containing many submissions.
 */
public class ProgrammingExercise extends ArtemisConnectionHolder implements Exercise {
    private final ProgrammingExerciseDTO dto;

    private final Course course;

    public ProgrammingExercise(ProgrammingExerciseDTO dto, Course course) {
        super(course);

        this.dto = dto;
        this.course = course;
    }

    @Override
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
        return AssessmentStatsDTO.fetch(this.getConnection().getClient(), this.getId());
    }

    public boolean hasSecondCorrectionRound() {
        return this.dto.secondCorrectionEnabled() != null && this.dto.secondCorrectionEnabled();
    }

    public List<ProgrammingSubmissionWithResults> fetchAllSubmissions() throws ArtemisNetworkException {
        // Artemis ignores the correction round since assessedByTutor is false
        return ProgrammingSubmissionDTO.fetchAll(this.getConnection().getClient(), this.getId(), 0, false).stream()
                .map(dto -> new ProgrammingSubmissionWithResults(new ProgrammingSubmission(dto, this)))
                .toList();
    }

    public List<PackedAssessment> fetchMyAssessments() throws ArtemisNetworkException {
        var assessments = new ArrayList<>(this.fetchMyAssessments(CorrectionRound.FIRST));
        if (this.hasSecondCorrectionRound()) {
            assessments.addAll(this.fetchMyAssessments(CorrectionRound.SECOND));
        }
        return assessments;
    }

    public List<PackedAssessment> fetchMyAssessments(CorrectionRound correctionRound) throws ArtemisNetworkException {
        if (correctionRound == CorrectionRound.SECOND && !this.hasSecondCorrectionRound()) {
            throw new IllegalArgumentException("This exercise does not have a second correction round");
        }

        if (correctionRound == CorrectionRound.REVIEW) {
            throw new IllegalArgumentException(
                    "Can't fetch assessments for the review 'round'. Instead fetch the assessments for the second review round and that open for the review round.");
        }

        var submissions =
                ProgrammingSubmissionDTO.fetchAll(
                                this.getConnection().getClient(), this.getId(), correctionRound.toArtemis(), true)
                        .stream()
                        .map(submissionDto -> new ProgrammingSubmission(submissionDto, this))
                        .toList();

        var assessments = new ArrayList<PackedAssessment>(submissions.size());
        for (var submission : submissions) {
            // For a non-instructor this returns only one result
            // For an instructor, this returns all results!
            // There is one caveat: Sometimes, there are multiple semiautomatic results
            // for the same correction round (no idea why). In this case, the following
            // logic breaks.
            var results = submission.getDTO().nonAutomaticResults();

            if (results.size() > 2) {
                throw new IllegalStateException("Submission %d has more than two non-automatic results: %s"
                        .formatted(submission.getId(), results));
            }

            ResultDTO relevantResult;
            if (results.size() == 1) {
                relevantResult = results.getFirst();
            } else {
                var assessor = this.getConnection().getAssessor();
                if (this.course.isInstructor(assessor)) {
                    // Instructors can see all results, so select the relevant one
                    relevantResult = results.get(correctionRound.toArtemis());
                } else {
                    throw new IllegalStateException("Too many non-automatic results for a non-instructor: "
                            + results.size() + ". Course " + getCourse().getId() + ", Exercise " + getId()
                            + ", Submission " + submission.getId());
                }
            }
            assessments.add(new PackedAssessment(relevantResult, correctionRound, submission));
        }
        return assessments;
    }

    /**
     * Tries to lock the next submission for this exercise. If successful, returns
     * the assessment.
     *
     * @return An empty optional if no submission was available to lock, otherwise
     * the assessment
     */
    public Optional<Assessment> tryLockNextSubmission(CorrectionRound correctionRound, GradingConfig gradingConfig)
            throws AnnotationMappingException, ArtemisNetworkException {
        this.assertGradingConfigValid(gradingConfig);

        // This line already locks the submission, but doesn't tell us what the relevant
        // ResultDTO is
        var nextSubmissionDto = ProgrammingSubmissionDTO.lockNextSubmission(
                this.getConnection().getClient(), this.getId(), correctionRound.toArtemis());
        if (nextSubmissionDto.isEmpty()) {
            return Optional.empty();
        }

        // Second lock call to get the ResultDTO
        try {
            var lockResult = this.tryLockSubmission(nextSubmissionDto.get().id(), correctionRound, gradingConfig);
            return Optional.of(lockResult.orElseThrow(IllegalStateException::new));
        } catch (MoreRecentSubmissionException ex) {
            // The student has submitted a new submission between our two lock calls
            // We assume that this doesn't happen to make downstream error handling simpler
            throw new IllegalStateException("A new submission has been created between successive locking calls", ex);
        }
    }

    /**
     * Tries to lock a specific submission for this exercise. Locking is reentrant,
     * i.e. a single user may lock the same submission multiple times.
     *
     * @return An empty optional if a *different* user has already locked the
     * submission, otherwise the assessment
     * @throws AnnotationMappingException    If the annotations that were already
     *                                       present could not be mapped given the
     *                                       gradingConfig
     * @throws ArtemisNetworkException       Generic network failure
     * @throws MoreRecentSubmissionException If the requested submission is not the
     *                                       most recent submission of the
     *                                       corresponding student (i.e.
     *                                       participation)
     */
    public Optional<Assessment> tryLockSubmission(
            long submissionId, CorrectionRound correctionRound, GradingConfig gradingConfig)
            throws AnnotationMappingException, ArtemisNetworkException, MoreRecentSubmissionException {
        this.assertGradingConfigValid(gradingConfig);

        var locked = ProgrammingSubmissionDTO.lock(
                this.getConnection().getClient(), submissionId, correctionRound.toArtemis());

        if (locked.id() != submissionId) {
            // Artemis automatically returns the most recent submission that is associated with the
            // participation of the requested submission
            throw new MoreRecentSubmissionException(
                    submissionId, locked.id(), locked.participation().id());
        }

        if (locked.results() == null) {
            throw new IllegalStateException("Locking returned a submission %d without results".formatted(submissionId));
        }

        if (locked.results().size() != 1) {
            throw new IllegalStateException("Locking returned %d results, expected 1"
                    .formatted(locked.results().size()));
        }
        var result = locked.results().getFirst();

        if (!this.canAssess(result)) {
            return Optional.empty();
        }

        var submission = new ProgrammingSubmission(locked, this);
        return Optional.of(new Assessment(result, gradingConfig, submission, correctionRound));
    }

    public int fetchMyAssessmentCount(CorrectionRound correctionRound) throws ArtemisNetworkException {
        return this.fetchMyAssessments(correctionRound).size();
    }

    public int fetchLockedSubmissionCount(CorrectionRound correctionRound) throws ArtemisNetworkException {
        return (int) this.fetchMyAssessments(correctionRound).stream()
                .filter(s -> !s.isSubmitted())
                .count();
    }

    @Override
    public String toString() {
        return this.getTitle();
    }

    private void assertGradingConfigValid(GradingConfig gradingConfig) {
        if (!gradingConfig.isValidForExercise(this)) {
            throw new IllegalArgumentException("Grading config is not valid for this exercise");
        }
    }

    private boolean canAssess(ResultDTO result) throws ArtemisNetworkException {
        // We can assess if either no assessor is set, we are the assessor,
        // or if we are an instructor (who can overwrite any assessment)
        var assessor = this.getConnection().getAssessor();
        return result.assessor() == null
                || result.assessor().id() == assessor.getId()
                || this.getCourse().isInstructor(assessor);
    }
}
