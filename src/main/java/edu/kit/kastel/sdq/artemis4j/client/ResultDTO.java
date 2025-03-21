/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
public record ResultDTO(
        @JsonProperty long id,
        @JsonProperty ZonedDateTime completionDate,
        @JsonProperty Boolean successful,
        @JsonProperty double score,
        @JsonProperty Boolean rated,
        @JsonProperty List<FeedbackDTO> feedbacks,
        @JsonProperty UserDTO assessor,
        @JsonProperty AssessmentType assessmentType,
        @JsonProperty int testCaseCount,
        @JsonProperty int passedTestCaseCount,
        @JsonProperty int codeIssueCount) {

    /**
     * BE WARNED: This method takes the (passed) test case count from the lockingResult. It does NOT recalculate it from the feedbacks!
     * Should you for any reason change the test result, DO NOT USE THIS METHOD!
     */
    public static ResultDTO forAssessmentSubmission(
            long submissionId, double score, List<FeedbackDTO> feedbacks, ResultDTO lockingResult) {
        return new ResultDTO(
                submissionId,
                null,
                true,
                score,
                true,
                feedbacks,
                lockingResult.assessor(),
                AssessmentType.SEMI_AUTOMATIC,
                lockingResult.testCaseCount(),
                lockingResult.passedTestCaseCount(),
                lockingResult.codeIssueCount());
    }

    private static List<FeedbackDTO> fetchFeedbacks(ArtemisClient client, long resultId, long participationId)
            throws ArtemisNetworkException {
        return Arrays.asList(ArtemisRequest.get()
                .path(List.of("assessment", "participations", participationId, "results", resultId, "details"))
                .executeAndDecode(client, FeedbackDTO[].class));
    }

    /**
     * This will fetch the feedbacks for this result and load the long feedbacks if
     * necessary.
     *
     * @param client          the client to use
     * @param resultId        the result id
     * @param participationId the participation id, likely
     *                        {@link ProgrammingSubmission#getParticipationId()}
     * @param feedbacks       the feedbacks to load the details for or null if they
     *                        should be fetched first
     * @return the feedbacks
     * @throws ArtemisNetworkException if it fails to fetch the feedbacks
     */
    public static List<FeedbackDTO> fetchDetailedFeedbacks(
            ArtemisClient client, long resultId, long participationId, List<FeedbackDTO> feedbacks)
            throws ArtemisNetworkException {
        // Sometimes the feedbacks are not loaded, to fetch the long feedbacks, we need
        // to load the feedbacks first
        if (feedbacks == null) {
            feedbacks = ResultDTO.fetchFeedbacks(client, resultId, participationId);
        }

        List<FeedbackDTO> cleanedFeedbacks = new ArrayList<>(feedbacks.size());
        for (var feedback : feedbacks) {
            if (feedback == null) {
                continue;
            }

            String detailText = feedback.detailText();
            if (feedback.hasLongFeedbackText()) {
                detailText = FeedbackDTO.fetchLongFeedback(client, feedback.id());
            }
            cleanedFeedbacks.add(new FeedbackDTO(detailText, feedback));
        }

        return cleanedFeedbacks;
    }
}
