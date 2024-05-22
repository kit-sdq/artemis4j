package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestCaseDTO(
        @JsonProperty int id,
        @JsonProperty String testName,
        @JsonProperty double weight,
        @JsonProperty boolean active,
        @JsonProperty String visibility,
        @JsonProperty double bonusMultiplier,
        @JsonProperty double bonusPoints,
        @JsonProperty String type
) {
}
