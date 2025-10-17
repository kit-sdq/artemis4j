/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.grading.Exercise;
import org.jspecify.annotations.Nullable;

public record ProgrammingSubmissionDTO(
        @JsonProperty long id,
        @JsonProperty ParticipationDTO participation,
        @JsonProperty String commitHash,
        @JsonProperty boolean buildFailed,
        @JsonProperty @Nullable List<ResultDTO> results,
        @JsonProperty ZonedDateTime submissionDate) {
    /**
     * Fetch all programming submissions for an exercise.
     *
     * @param client                the artemis client to use
     * @param exerciseId            the id of the exercise {@link Exercise#getId()}
     * @param correctionRound       the correction round, 0 for the first correction
     *                              round or if there is only one correction round
     * @param filterAssessedByTutor whether to only fetch submissions that have been
     *                              assessed by the currently logged-in user/tutor
     * @return a list of programming submissions
     * @throws ArtemisNetworkException if the request fails
     */
    public static List<ProgrammingSubmissionDTO> fetchAll(
            ArtemisClient client, long exerciseId, int correctionRound, boolean filterAssessedByTutor)
            throws ArtemisNetworkException {
        return new ArrayList<>(Arrays.asList(ArtemisRequest.get()
                .path(List.of("programming", "exercises", exerciseId, "programming-submissions"))
                .param("assessedByTutor", filterAssessedByTutor)
                .param("correction-round", correctionRound)
                .executeAndDecode(client, ProgrammingSubmissionDTO[].class)));
    }

    public static ProgrammingSubmissionDTO lock(ArtemisClient client, long submissionId, int correctionRound)
            throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("programming", "programming-submissions", submissionId, "lock"))
                .param("correction-round", correctionRound)
                .executeAndDecode(client, ProgrammingSubmissionDTO.class);
    }

    public static Optional<ProgrammingSubmissionDTO> lockNextSubmission(
            ArtemisClient client, long exerciseId, int correctionRound) throws ArtemisNetworkException {
        // Artemis returns an empty string if there is no new submission to lock
        return ArtemisRequest.get()
                .path(List.of("programming", "exercises", exerciseId, "programming-submission-without-assessment"))
                .param("lock", true)
                .param("correction-round", correctionRound)
                .executeAndDecodeMaybe(client, ProgrammingSubmissionDTO.class);
    }

    public static void cancelAssessment(ArtemisClient client, long submissionId) throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("programming", "programming-submissions", submissionId, "cancel-assessment"))
                .execute(client);
    }

    public static void saveAssessment(ArtemisClient client, long participationId, boolean submit, ResultDTO result)
            throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("programming", "participations", participationId, "manual-results"))
                .param("submit", submit)
                .body(result)
                .execute(client);
    }

    public List<ResultDTO> nonAutomaticResults() {
        if (this.results == null) {
            return List.of();
        }

        return this.results().stream()
                .filter(r -> r != null && r.assessmentType() != AssessmentType.AUTOMATIC)
                .toList();
    }
}
