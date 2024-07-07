/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TestCaseDTO(@JsonProperty int id, @JsonProperty String testName, @JsonProperty double weight, @JsonProperty boolean active,
		@JsonProperty String visibility, @JsonProperty double bonusMultiplier, @JsonProperty double bonusPoints, @JsonProperty String type) {
}
