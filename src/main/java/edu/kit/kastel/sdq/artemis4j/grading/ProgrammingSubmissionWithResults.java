package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Objects;

public class ProgrammingSubmissionWithResults {
    private final ProgrammingSubmission submission;
    private final PackedAssessment firstRoundAssessment;
    private final PackedAssessment secondRoundAssessment;

    public ProgrammingSubmissionWithResults(ProgrammingSubmission submission) {
        this.submission = submission;

        var results = submission.getDTO().nonAutomaticResults();
        if (results.isEmpty()) {
            this.firstRoundAssessment = null;
            this.secondRoundAssessment = null;
        } else if (results.size() == 1) {
            this.firstRoundAssessment = new PackedAssessment(results.get(0), CorrectionRound.FIRST, submission);
            this.secondRoundAssessment = null;
        } else if (results.size() == 2){
            this.firstRoundAssessment = new PackedAssessment(results.get(0), CorrectionRound.FIRST, submission);
            this.secondRoundAssessment = new PackedAssessment(results.get(1), CorrectionRound.SECOND, submission);
        } else {
            throw new IllegalStateException("Submission has more than two non-automatic results");
        }
    }

    public ProgrammingSubmission getSubmission() {
        return submission;
    }

    public PackedAssessment getFirstRoundAssessment() {
        return firstRoundAssessment;
    }

    public PackedAssessment getSecondRoundAssessment() {
        return secondRoundAssessment;
    }
}
