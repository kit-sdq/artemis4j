package edu.kit.kastel.sdq.artemis4j.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record SubmissionDTO(
        @JsonProperty long id,
        @JsonProperty ParticipationDTO participation,
        @JsonProperty String commitHash,
        @JsonProperty boolean buildFailed,
        @JsonProperty ResultDTO[] results,
        @JsonProperty ZonedDateTime submissionDate,
        @JsonProperty UserDTO user
) {

    public static List<SubmissionDTO> fetchAll(ArtemisClient client, long exerciseId, int correctionRound, boolean filterAssessedByTutor) throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get()
                .path(List.of("exercises", exerciseId, "programming-submissions"))
                .param("assessedByTutor", filterAssessedByTutor)
                .param("correction-round", correctionRound)
                .executeAndDecode(client, SubmissionDTO[].class);

        for (var submission : submissions) {
            submission.fetchLongFeedback(client);
        }

        return List.of(submissions);
    }

    public static SubmissionDTO lock(ArtemisClient client, long submissionId, int correctionRound) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("programming-submissions", submissionId, "lock"))
                .param("correction-round", correctionRound)
                .executeAndDecode(client, SubmissionDTO.class)
                .fetchLongFeedback(client);
    }

    public static Optional<SubmissionDTO> lockNextSubmission(ArtemisClient client, long exerciseId, int correctionRound) throws ArtemisNetworkException {
        // Artemis returns an empty string if there is no new submission to lock
        var submission = ArtemisRequest.get()
                .path(List.of("exercises", exerciseId, "programming-submission-without-assessment"))
                .param("lock", true)
                .param("correction-round", correctionRound)
                .executeAndDecodeMaybe(client, SubmissionDTO.class);

        if (submission.isPresent()) {
            submission = Optional.of(submission.get().fetchLongFeedback(client));
        }

        return submission;
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

    public SubmissionDTO fetchLongFeedback(ArtemisClient client) throws ArtemisNetworkException {
        for (var result : this.results()) {
            for (int i = 0; i < result.feedbacks().length; i++) {
                var feedback = result.feedbacks()[i];
                if (feedback.hasLongFeedbackText()) {
                    String detailText = FeedbackDTO.fetchLongFeedback(client, result.id(), feedback.id());
                    result.feedbacks()[i] = new FeedbackDTO(
                            feedback.type(),
                            feedback.id(),
                            feedback.credits(),
                            feedback.positive(),
                            feedback.visibility(),
                            feedback.text(),
                            feedback.reference(),
                            detailText,
                            true,
                            feedback.testCase());
                }
            }
        }
        return this;
    }
}
