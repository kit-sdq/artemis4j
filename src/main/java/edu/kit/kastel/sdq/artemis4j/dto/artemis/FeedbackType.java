package edu.kit.kastel.sdq.artemis4j.dto.artemis;

public enum FeedbackType {
    /**
     * Single-line manual annotation
     */
    MANUAL,

    /**
     * Manual annotation that is not associated with a specific line
     */
    MANUAL_UNREFERENCED,

    /**
     * E.g. unit test result
     */
    AUTOMATIC,
}
