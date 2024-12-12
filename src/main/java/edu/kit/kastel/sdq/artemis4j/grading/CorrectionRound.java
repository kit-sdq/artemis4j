/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.client.AnnotationSource;

public enum CorrectionRound {
    FIRST,
    SECOND,
    REVIEW;

    public int toArtemis() {
        return switch (this) {
            case FIRST -> 0;
            case SECOND, REVIEW -> 1;
        };
    }

    public AnnotationSource toAnnotationSource() {
        return switch (this) {
            case FIRST -> AnnotationSource.MANUAL_FIRST_ROUND;
            case SECOND -> AnnotationSource.MANUAL_SECOND_ROUND;
            case REVIEW -> AnnotationSource.REVIEW;
        };
    }
}
