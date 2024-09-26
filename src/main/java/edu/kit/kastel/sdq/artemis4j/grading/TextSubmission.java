/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextBlockDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextSubmissionDTO;

/**
 * A text submission for a {@link TextExercise}.
 */
public class TextSubmission extends ArtemisConnectionHolder {
    private final TextSubmissionDTO dto;

    private final int correctionRound;
    private final User student;
    private final TextExercise exercise;

    /**
     * Creates a new text submission.
     *
     * @param dto the DTO for this submission
     * @param exercise the exercise this submission is for
     * @param correctionRound the correction round this submission is part of
     */
    public TextSubmission(TextSubmissionDTO dto, TextExercise exercise, int correctionRound) {
        super(exercise);

        this.dto = dto;
        this.exercise = exercise;

        // The student is only present for instructors
        if (dto.participation() != null) {
            var studentDto = dto.participation().student();
            if (studentDto != null) {
                this.student = new User(studentDto);
            } else {
                this.student = null;
            }
        } else {
            this.student = null;
        }

        this.correctionRound = correctionRound;
    }

    /**
     * Get the ID of the submission.
     * @return the ID of the submission
     */
    public long getId() {
        return this.dto.id();
    }

    public long getParticipationId() {
        return this.dto.participation().id();
    }

    public String getParticipantIdentifier() {
        return this.dto.participation().participantIdentifier();
    }

    /**
     * The student can only be retrieved by instructors.
     */
    public Optional<User> getStudent() {
        return Optional.ofNullable(this.student);
    }

    public TextExercise getExercise() {
        return exercise;
    }

    public int getCorrectionRound() {
        return this.correctionRound;
    }

    public ZonedDateTime getSubmissionDate() {
        return this.dto.submissionDate();
    }

    /**
     * Get the text of the submission.
     *
     * @return the text that has been submitted
     */
    public String getText() {
        return Optional.ofNullable(this.dto.text()).orElse("");
    }

    public Optional<ResultDTO> getLatestResult() {
        if (this.dto.results().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(this.dto.results().get(this.dto.results().size() - 1));
        }
    }

    List<TextBlockDTO> getTextBlocks() {
        return this.dto.blocks();
    }

    public TextAssessment openAssessment() throws ArtemisNetworkException {
        TextSubmissionDTO submissionWithResults =
                TextSubmissionDTO.openAssessment(this.getConnection().getClient(), this.getId(), this.correctionRound);

        // for some reason the participation is not included in the response, so we have to add it manually:
        submissionWithResults = new TextSubmissionDTO(
                submissionWithResults.id(),
                submissionWithResults.submitted(),
                this.dto.participation(),
                submissionWithResults.commitHash(),
                submissionWithResults.buildFailed(),
                submissionWithResults.results(),
                submissionWithResults.submissionDate(),
                submissionWithResults.text(),
                submissionWithResults.blocks());

        return new TextAssessment(
                new TextSubmission(submissionWithResults, this.exercise, this.correctionRound), this.correctionRound);
    }
}
