package edu.kit.kastel.sdq.artemis4j.new_api.penalty;

public enum MistakeReportingState {
    REPORT,
    REPORT_AND_SCORE;

    public boolean shouldScore() {
        return this == REPORT_AND_SCORE;
    }
}
