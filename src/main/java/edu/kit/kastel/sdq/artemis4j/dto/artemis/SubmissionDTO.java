package edu.kit.kastel.sdq.artemis4j.dto.artemis;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.new_client.ArtemisRequest;

import java.time.ZonedDateTime;
import java.util.List;

public record SubmissionDTO(
        @JsonProperty long id,
        @JsonProperty ParticipationDTO participation,
        @JsonProperty String commitHash,
        @JsonProperty boolean buildFailed,
        @JsonProperty ResultDTO[] results,
        @JsonProperty ZonedDateTime submissionDate,
        @JsonProperty UserDTO user
) {

    public static SubmissionDTO fetch(ArtemisClient client, long submissionId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("submissions", submissionId))
                .executeAndDecode(client, SubmissionDTO.class);
    }

    public static List<SubmissionDTO> fetchAll(ArtemisClient client, int exerciseId, int correctionRound, boolean filterAssessedByTutor) throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get()
                .path(List.of("exercises", exerciseId, "programming-submissions"))
                .param("assessedByTutor", filterAssessedByTutor)
                .param("correction-round", correctionRound)
                .executeAndDecode(client, SubmissionDTO[].class);
        return List.of(submissions);
    }

    public static SubmissionDTO lock(ArtemisClient client, long submissionId, int correctionRound) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("programming-submissions", submissionId, "lock"))
                .param("correction-round", correctionRound)
                .executeAndDecode(client, SubmissionDTO.class);
    }

    public static void cancelAssessment(ArtemisClient client, long submissionId) throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("programming-submissions", submissionId, "cancel-assessment"))
                .execute(client);
    }

    public static void saveAssessment(ArtemisClient client, long participationId, boolean submit, ResultDTO result) throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("participations", participationId, "manual-results"))
                .param("submit", submit)
                .body(result)
                .execute(client);
    }
}
