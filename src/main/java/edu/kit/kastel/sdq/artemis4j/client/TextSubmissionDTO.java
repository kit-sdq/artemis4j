/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.Exercise;

public record TextSubmissionDTO(
        @JsonProperty long id,
        @JsonProperty boolean submitted,
        @JsonProperty ParticipationDTO participation,
        @JsonProperty String commitHash,
        @JsonProperty boolean buildFailed,
        @JsonProperty List<ResultDTO> results,
        @JsonProperty ZonedDateTime submissionDate,
        @JsonProperty String text,
        @JsonProperty List<TextBlockDTO> blocks) {
    /**
     * Fetch all text submissions for an exercise.
     *
     * @param client                the artemis client to use
     * @param exerciseId            the id of the exercise {@link Exercise#getId()}
     * @param correctionRound       the correction round, 0 for the first correction
     *                              round or if there is only one correction round
     * @param filterAssessedByTutor whether to only fetch submissions that have been
     *                              assessed by the currently logged-in user/tutor
     * @return a list of submissions
     * @throws ArtemisNetworkException if the request fails
     */
    public static List<TextSubmissionDTO> fetchAll(
            ArtemisClient client, long exerciseId, int correctionRound, boolean filterAssessedByTutor)
            throws ArtemisNetworkException {
        return new ArrayList<>(Arrays.asList(ArtemisRequest.get()
                .path(List.of("exercises", exerciseId, "text-submissions"))
                .param("assessedByTutor", filterAssessedByTutor)
                .param("correction-round", correctionRound)
                .executeAndDecode(client, TextSubmissionDTO[].class)));
    }

    /**
     * Try to lock a (not already locked) submission for assessment.
     *
     * @param client the artemis client to use
     * @param exerciseId the id of the exercise {@link Exercise#getId()}
     * @param correctionRound the correction round, 0 for the first correction round or if there is only one correction round
     * @return the locked submission or an empty optional if there is no submission to lock at the moment
     * @throws ArtemisNetworkException if the request fails
     */
    public static Optional<TextSubmissionDTO> lockNextSubmission(
            ArtemisClient client, long exerciseId, int correctionRound) throws ArtemisNetworkException {
        // Artemis returns an empty string if there is no new submission to lock
        return ArtemisRequest.get()
                .path(List.of("exercises", exerciseId, "text-submission-without-assessment"))
                .param("lock", true)
                .param("correction-round", correctionRound)
                .executeAndDecodeMaybe(client, TextSubmissionDTO.class);
    }

    public static TextSubmissionDTO openAssessment(ArtemisClient client, long submissionId, int correctionRound)
            throws ArtemisNetworkException {
        var res = ArtemisRequest.get()
                .path(List.of("text-submissions", submissionId, "for-assessment"))
                .param("correction-round", correctionRound)
                .executeAndDecode(client, InternalOpenAssessmentDTO.class);

        return res.submissions().get(0);
    }

    // This is what artemis returns for the request, doesn't seem to map to any existing DTO.
    // It is only used for the openAssessment method, therefore it is kept private for now.
    private record InternalOpenAssessmentDTO(
            @JsonProperty String type,
            // this is the participationId
            @JsonProperty long id,
            @JsonProperty TextExerciseDTO exercise,
            @JsonProperty List<ResultDTO> results,
            @JsonProperty List<TextSubmissionDTO> submissions,
            @JsonProperty UserDTO student,
            @JsonProperty String participantIdentifier,
            @JsonProperty String participantName) {}

    /**
     * Cancel the assessment of the specified submission.
     *
     * @param client the artemis client to use
     * @param submissionId the id of the submission
     * @param participationId the id of the participation
     * @throws ArtemisNetworkException if the request fails
     */
    public static void cancelAssessment(ArtemisClient client, long submissionId, long participationId)
            throws ArtemisNetworkException {
        ArtemisRequest.post()
                .path(List.of("participations", participationId, "submissions", submissionId, "cancel-assessment"))
                .execute(client);
    }

    /**
     * Save and submit the assessment for the specified submission.
     *
     * @param client the artemis client to use
     * @param participationId the id of the participation
     * @param resultId the id of the result
     * @param feedbacks the feedbacks to include in the assessment
     * @param textBlocks the text blocks to include in the assessment
     * @throws ArtemisNetworkException if the request fails
     */
    @SuppressWarnings("java:S1171")
    public static void submitAssessment(
            ArtemisClient client,
            long participationId,
            long resultId,
            List<FeedbackDTO> feedbacks,
            List<TextBlockDTO> textBlocks)
            throws ArtemisNetworkException {
        ArtemisRequest.post()
                .path(List.of("participations", participationId, "results", resultId, "submit-text-assessment"))
                .body(new HashMap<>() {
                    {
                        this.put("feedbacks", feedbacks);
                        this.put("textBlocks", textBlocks);
                    }
                })
                .execute(client);
    }
}
