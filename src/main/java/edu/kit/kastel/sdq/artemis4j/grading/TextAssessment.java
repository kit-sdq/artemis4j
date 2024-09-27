/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.client.FeedbackType;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextBlockDTO;
import edu.kit.kastel.sdq.artemis4j.client.TextSubmissionDTO;

/**
 * An assessment for a text submission.
 */
public class TextAssessment extends ArtemisConnectionHolder {
    private final ResultDTO resultDTO;
    private final TextSubmission submission;
    private final int correctionRound;

    private final List<TextAnnotation> annotations;

    TextAssessment(ResultDTO resultDTO, TextSubmission submission, int correctionRound) {
        super(submission.getExercise());

        this.resultDTO = resultDTO;
        this.submission = submission;
        this.correctionRound = correctionRound;

        this.annotations = new ArrayList<>();
    }

    TextAssessment(TextSubmission submission, int correctionRound) {
        super(submission.getExercise());

        this.resultDTO = submission
                .getLatestResult()
                .orElseThrow(() -> new IllegalStateException("No result found for submission."));
        this.submission = submission;
        this.correctionRound = correctionRound;

        this.annotations = new ArrayList<>();

        Collection<FeedbackDTO> feedbacks = new ArrayList<>();
        if (this.resultDTO.feedbacks() != null) {
            feedbacks.addAll(this.resultDTO.feedbacks());
        }

        for (FeedbackDTO feedback : feedbacks) {
            this.annotations.add(TextAnnotation.unpack(feedback, this.submission.getTextBlocks()));
        }
    }

    public String getText() {
        return this.submission.getText();
    }

    public int getCorrectionRound() {
        return this.correctionRound;
    }

    /**
     * Adds a feedback to the assessment.
     *
     * @param credits    the amount of credits to give
     * @param detailText the feedback text for the student
     * @param startIndex where in the text the feedback should be placed (inclusive)
     * @param endIndex   where in the text the feedback should end (not inclusive)
     */
    public void addAnnotation(double credits, String detailText, int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex > this.getText().length()) {
            throw new IllegalArgumentException("The annotation must be within the text bounds.");
        }

        this.annotations.add(new TextAnnotation(credits, detailText, startIndex, endIndex));
    }

    /**
     * Clears all annotations.
     */
    public void clearAnnotations() {
        this.annotations.clear();
    }

    /**
     * Get the annotations that are present. Use the add/remove methods to modify
     * the list of annotations.
     *
     * @return An unmodifiable list of annotations.
     */
    public List<TextAnnotation> getAnnotations() {
        return Collections.unmodifiableList(this.annotations);
    }

    /**
     * Submits the assessment to Artemis.
     *
     * @throws ArtemisNetworkException if the request fails
     */
    public void submit() throws ArtemisNetworkException {
        List<TextBlockDTO> textBlocks = new ArrayList<>();
        List<FeedbackDTO> feedbacks = new ArrayList<>();

        for (TextAnnotation annotation : this.annotations) {
            String textSegment = this.getText().substring(annotation.startIndex(), annotation.endIndex());
            TextBlockDTO textBlock = new TextBlockDTO(
                    this.submission.getId(),
                    textSegment,
                    annotation.startIndex(),
                    annotation.endIndex(),
                    0,
                    FeedbackType.AUTOMATIC);
            textBlocks.add(textBlock);
            feedbacks.add(FeedbackDTO.newManual(annotation.credits(), null, textBlock.id(), annotation.detailText()));
        }

        TextSubmissionDTO.submitAssessment(
                this.getConnection().getClient(),
                this.submission.getParticipationId(),
                this.resultDTO.id(),
                feedbacks,
                textBlocks);
    }

    /**
     * Cancels the assessment.
     *
     * @throws ArtemisNetworkException if the request fails
     */
    public void cancel() throws ArtemisNetworkException {
        TextSubmissionDTO.cancelAssessment(
                this.getConnection().getClient(), this.submission.getId(), this.submission.getParticipationId());
    }
}
