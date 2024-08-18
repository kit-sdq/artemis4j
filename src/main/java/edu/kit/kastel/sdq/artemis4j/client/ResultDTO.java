/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingSubmission;

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

    private List<FeedbackDTO> fetchFeedbacks(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        if (this.feedbacks != null) {
            return Collections.unmodifiableList(this.feedbacks);
        }

        return Arrays.asList(ArtemisRequest.get().path(List.of("participations", participationId, "results", this.id(), "details")).executeAndDecode(client,
                FeedbackDTO[].class));
    }

    /**
     * This will fetch the feedbacks for this result and load the long feedbacks if
     * necessary.
     *
     * @param client          the client to use
     * @param participationId the participation id, likely
     *                        {@link ProgrammingSubmission#getParticipationId()}
     * @return the feedbacks
     * @throws ArtemisNetworkException if it fails to fetch the feedbacks
     */
    public List<FeedbackDTO> fetchDetailedFeedbacks(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        // Sometimes the feedbacks are not loaded, to fetch the long feedbacks, we need
        // to load the feedbacks first
        List<FeedbackDTO> feedbacks = this.fetchFeedbacks(client, participationId);

        List<FeedbackDTO> cleanedFeedbacks = new ArrayList<>(feedbacks.size());
        for (var feedback : feedbacks) {
            if (feedback == null) {
                continue;
            }

            String detailText = feedback.detailText();
            if (feedback.hasLongFeedbackText()) {
                detailText = FeedbackDTO.fetchLongFeedback(client, this.id(), feedback.id());
            }
            cleanedFeedbacks.add(new FeedbackDTO(detailText, feedback));
        }

        return cleanedFeedbacks;
    }
}
