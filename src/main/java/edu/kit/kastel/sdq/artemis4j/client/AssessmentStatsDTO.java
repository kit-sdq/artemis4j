/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record AssessmentStatsDTO(
        @JsonProperty Timing numberOfSubmissions,
        @JsonProperty List<Timing> numberOfAssessmentsOfCorrectionRounds,
        @JsonProperty int totalNumberOfAssessmentLocks) {

    public static AssessmentStatsDTO fetch(ArtemisClient client, long exerciseId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("exercise", "exercises", exerciseId, "stats-for-assessment-dashboard"))
                .executeAndDecode(client, AssessmentStatsDTO.class);
    }

    public record Timing(@JsonProperty int inTime, @JsonProperty int late) {
        public int total() {
            return inTime + late;
        }
    }
}
