package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record ResultDTO(
        @JsonProperty long id,
        @JsonProperty ZonedDateTime completionDate,
        @JsonProperty Boolean successful,
        @JsonProperty double score,
        @JsonProperty Boolean rated,
        @JsonProperty FeedbackDTO[] feedbacks,
        @JsonProperty UserDTO assessor
        ) {

        public static ResultDTO forAssessmentSubmission(long submissionId, double score, FeedbackDTO[] feedbacks, UserDTO assessor) {
            return new ResultDTO(submissionId, null, true, score, true, feedbacks, assessor);
        }
}
