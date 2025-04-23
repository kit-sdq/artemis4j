/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.AssessmentType;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;

public class ProgrammingSubmissionWithResults {
    private final ProgrammingSubmission submission;
    private final ResultDTO automaticResult;
    private final PackedAssessment firstRoundAssessment;
    private final PackedAssessment secondRoundAssessment;
    private final PackedAssessment reviewAssessment;

    public ProgrammingSubmissionWithResults(ProgrammingSubmission submission) {
        this.submission = submission;

        // Find the latest automatic result
        ResultDTO automaticResult = null;
        for (var result : submission.getDTO().results()) {
            if (result.assessmentType() == AssessmentType.AUTOMATIC) {
                automaticResult = result;
            }
        }
        this.automaticResult = automaticResult;

        var results = submission.getDTO().nonAutomaticResults();
        if (results.isEmpty()) {
            this.firstRoundAssessment = null;
            this.secondRoundAssessment = null;
            this.reviewAssessment = null;
        } else if (results.size() == 1) {
            this.firstRoundAssessment = new PackedAssessment(results.get(0), CorrectionRound.FIRST, submission);
            this.secondRoundAssessment = null;
            this.reviewAssessment = null;
        } else if (results.size() == 2) {
            this.firstRoundAssessment = new PackedAssessment(results.get(0), CorrectionRound.FIRST, submission);
            this.secondRoundAssessment = new PackedAssessment(results.get(1), CorrectionRound.SECOND, submission);

            if (secondRoundAssessment.isSubmitted()) {
                reviewAssessment = new PackedAssessment(results.get(1), CorrectionRound.REVIEW, submission);
            } else {
                reviewAssessment = null;
            }
        } else {
            throw new IllegalStateException("Submission has more than two non-automatic results");
        }
    }

    public ProgrammingSubmission getSubmission() {
        return submission;
    }

    public Optional<ResultDTO> getAutomaticResult() {
        return Optional.ofNullable(automaticResult);
    }

    public PackedAssessment getFirstRoundAssessment() {
        return firstRoundAssessment;
    }

    public PackedAssessment getSecondRoundAssessment() {
        return secondRoundAssessment;
    }

    public PackedAssessment getReviewAssessment() {
        return reviewAssessment;
    }
}
