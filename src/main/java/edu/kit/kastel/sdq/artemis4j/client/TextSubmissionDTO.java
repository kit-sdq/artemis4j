/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.Exercise;
import org.jspecify.annotations.Nullable;

public record TextSubmissionDTO(
        @JsonProperty long id,
        @JsonProperty boolean submitted,
        @JsonProperty @Nullable ParticipationDTO participation,
        @JsonProperty @Nullable List<ResultDTO> results,
        @JsonProperty @Nullable ZonedDateTime submissionDate,
        @JsonProperty @Nullable String text,
        @JsonProperty @Nullable List<TextBlockDTO> blocks) {
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
                .path(List.of("text", "exercises", exerciseId, "text-submissions"))
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
                .path(List.of("text", "exercises", exerciseId, "text-submission-without-assessment"))
                .param("lock", true)
                .param("correction-round", correctionRound)
                .executeAndDecodeMaybe(client, InternalTextSubmissionWithoutAssessmentDTO.class)
                .map(locked -> locked.participation().toTextSubmissionDTO());
    }

    public static TextSubmissionDTO openAssessment(ArtemisClient client, long submissionId, int correctionRound)
            throws ArtemisNetworkException {
        var participation = ArtemisRequest.get()
                .path(List.of("text", "text-submissions", submissionId, "for-assessment"))
                .param("correction-round", correctionRound)
                .executeAndDecode(client, InternalTextParticipationDTO.class);

        return participation.toTextSubmissionDTO();
    }

    private record InternalTextParticipationDTO(
            @JsonProperty long id,
            @JsonProperty @Nullable UserDTO student,
            @JsonProperty List<TextSubmissionDTO> submissions) {
        private TextSubmissionDTO toTextSubmissionDTO() {
            var submission = submissions.getFirst();
            var participation = new ParticipationDTO(id, student, null, null, null, null, null);
            return new TextSubmissionDTO(
                    submission.id(),
                    submission.submitted(),
                    participation,
                    submission.results(),
                    submission.submissionDate(),
                    submission.text(),
                    submission.blocks());
        }
    }

    private record InternalTextSubmissionWithoutAssessmentDTO(
            @JsonProperty InternalTextParticipationDTO participation) {}

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
                .path(List.of(
                        "text", "participations", participationId, "submissions", submissionId, "cancel-assessment"))
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
    public static void submitAssessment(
            ArtemisClient client,
            long participationId,
            long resultId,
            List<FeedbackDTO> feedbacks,
            List<TextBlockDTO> textBlocks)
            throws ArtemisNetworkException {
        ArtemisRequest.post()
                .path(List.of("text", "participations", participationId, "results", resultId, "submit-text-assessment"))
                .body(Map.of(
                        "feedbacks", feedbacks,
                        "textBlocks", textBlocks))
                .execute(client);
    }
}
