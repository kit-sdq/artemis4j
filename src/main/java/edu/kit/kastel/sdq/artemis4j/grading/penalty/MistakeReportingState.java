package edu.kit.kastel.sdq.artemis4j.grading.penalty;

/**
 * Specifies how annotated mistakes should be reported to the student.
 */
public enum MistakeReportingState {
    /**
     * The mistake should only be reported, but no points should be given or deducted
     */
    REPORT,

    /**
     * The mistake should be reported, and points should be given or deducted
     */
    REPORT_AND_SCORE;

    /**
     *
     * @return whether the mistake should score
     */
    public boolean shouldScore() {
        return this == REPORT_AND_SCORE;
    }
}
