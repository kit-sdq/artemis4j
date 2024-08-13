/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

/**
 * A result of a student's submission.
 *
 * @param id             the id of the result
 * @param completionDate the date when the result was completed
 * @param successful     whether the submission executed successfully
 * @param score          the score of the submission
 * @param rated          whether the submission was rated
 * @param feedbacks      the feedbacks given for the submission, might be null
 * @param assessor       the user who assessed the submission or null
 */
public record ResultDTO(@JsonProperty long id, @JsonProperty ZonedDateTime completionDate, @JsonProperty Boolean successful, @JsonProperty double score,
        @JsonProperty Boolean rated, @JsonProperty List<FeedbackDTO> feedbacks, @JsonProperty UserDTO assessor, @JsonProperty AssessmentType assessmentType) {

    public static ResultDTO forAssessmentSubmission(long submissionId, double score, List<FeedbackDTO> feedbacks, UserDTO assessor) {
        return new ResultDTO(submissionId, null, true, score, true, feedbacks, assessor, AssessmentType.SEMI_AUTOMATIC);
    }

    public ResultDTO fetchFeedbacks(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        if (this.feedbacks != null) {
            return this;
        }

        List<FeedbackDTO> feedbacks = Arrays.asList(ArtemisRequest.get().path(List.of("participations",  participationId, "results", this.id(), "details")).executeAndDecode(client, FeedbackDTO[].class));

        return new ResultDTO(this.id, this.completionDate, this.successful, this.score, this.rated, feedbacks, this.assessor, this.assessmentType);
    }
}
