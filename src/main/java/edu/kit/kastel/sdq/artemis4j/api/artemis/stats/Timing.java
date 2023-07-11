/* Licensed under EPL-2.0 2022. */
package edu.kit.kastel.sdq.artemis4j.api.artemis.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Timing(@JsonProperty int inTime, @JsonProperty int late) {
}
