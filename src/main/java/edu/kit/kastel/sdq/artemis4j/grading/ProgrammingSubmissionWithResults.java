/* Licensed under EPL-2.0 2025. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.AssessmentType;
import edu.kit.kastel.sdq.artemis4j.client.ResultDTO;
import org.jspecify.annotations.Nullable;

public class ProgrammingSubmissionWithResults {
    private final ProgrammingSubmission submission;
    private final @Nullable ResultDTO automaticResult;
    private final @Nullable ResultDTO firstRoundResult;
    private final @Nullable ResultDTO secondRoundResult;

    public ProgrammingSubmissionWithResults(ProgrammingSubmission submission) {
        this.submission = submission;

        // Find the latest automatic result
        ResultDTO automaticResult = null;
        // the results are sometimes null if there is not even an automatic result (yet)
        if (submission.getDTO().results() != null) {
            for (var result : submission.getDTO().results()) {
                if (result.assessmentType() == AssessmentType.AUTOMATIC) {
                    automaticResult = result;
                }
            }
        }
        this.automaticResult = automaticResult;

        var results = submission.getDTO().nonAutomaticResults();
        if (results.isEmpty()) {
            this.firstRoundResult = null;
            this.secondRoundResult = null;
        } else if (results.size() == 1) {
            this.firstRoundResult = results.getFirst();
            this.secondRoundResult = null;
        } else if (results.size() == 2) {
            this.firstRoundResult = results.get(0);
            this.secondRoundResult = results.get(1);
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

    public boolean isFirstRoundStarted() {
        return firstRoundResult != null;
    }

    public boolean isFirstRoundFinished() {
        return firstRoundResult != null && firstRoundResult.completionDate() != null;
    }

    public @Nullable PackedAssessment getFirstRoundAssessment() {
        if (firstRoundResult == null) {
            return null;
        }
        return new PackedAssessment(firstRoundResult, CorrectionRound.FIRST, submission);
    }

    public boolean isSecondRoundStarted() {
        return secondRoundResult != null;
    }

    public boolean isSecondRoundFinished() {
        return secondRoundResult != null && secondRoundResult.completionDate() != null;
    }

    public @Nullable PackedAssessment getSecondRoundAssessment() {
        if (secondRoundResult == null) {
            return null;
        }
        return new PackedAssessment(secondRoundResult, CorrectionRound.SECOND, submission);
    }

    public @Nullable PackedAssessment getReviewAssessment() {
        if (secondRoundResult == null) {
            return null;
        }

        return new PackedAssessment(secondRoundResult, CorrectionRound.REVIEW, submission);
    }
}
