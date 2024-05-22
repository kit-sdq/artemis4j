package edu.kit.kastel.sdq.artemis4j.new_api;

import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.FeedbackType;

public class TestResult {
    private final long id;
    private final FeedbackDTO dto;

    public TestResult(FeedbackDTO dto) {
        if (dto.type() != FeedbackType.AUTOMATIC) {
            throw new IllegalArgumentException("Feedback type must be automatic for test results");
        }

        this.id = dto.id();
        this.dto = dto;
    }

    public double getPoints() {
        return this.dto.credits();
    }

    protected FeedbackDTO getDto() {
        return this.dto;
    }
}
