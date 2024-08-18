/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;

public record ProgrammingSubmissionDTO(@JsonProperty long id, @JsonProperty ParticipationDTO participation, @JsonProperty String commitHash,
        @JsonProperty boolean buildFailed, @JsonProperty List<ResultDTO> results, @JsonProperty ZonedDateTime submissionDate) {

    public static List<ProgrammingSubmissionDTO> fetchAll(ArtemisClient client, long exerciseId, int correctionRound, boolean filterAssessedByTutor)
            throws ArtemisNetworkException {
        var submissions = ArtemisRequest.get().path(List.of("exercises", exerciseId, "programming-submissions")).param("assessedByTutor", filterAssessedByTutor)
                .param("correction-round", correctionRound).executeAndDecode(client, ProgrammingSubmissionDTO[].class);

        List<ProgrammingSubmissionDTO> result = new ArrayList<>();
        for (var submission : submissions) {
            result.add(submission.fetchLongFeedback(client));
        }

        return result;
    }

    public static ProgrammingSubmissionDTO lock(ArtemisClient client, long submissionId, int correctionRound) throws ArtemisNetworkException {
        return ArtemisRequest.get().path(List.of("programming-submissions", submissionId, "lock")).param("correction-round", correctionRound)
                .executeAndDecode(client, ProgrammingSubmissionDTO.class).fetchLongFeedback(client);
    }

    public static Optional<ProgrammingSubmissionDTO> lockNextSubmission(ArtemisClient client, long exerciseId, int correctionRound)
            throws ArtemisNetworkException {
        // Artemis returns an empty string if there is no new submission to lock
        var submission = ArtemisRequest.get().path(List.of("exercises", exerciseId, "programming-submission-without-assessment")).param("lock", true)
                .param("correction-round", correctionRound).executeAndDecodeMaybe(client, ProgrammingSubmissionDTO.class);

        if (submission.isPresent()) {
            submission = Optional.of(submission.get().fetchLongFeedback(client));
        }

        return submission;
    }

    public static void cancelAssessment(ArtemisClient client, long submissionId) throws ArtemisNetworkException {
        ArtemisRequest.put().path(List.of("programming-submissions", submissionId, "cancel-assessment")).execute(client);
    }

    public static void saveAssessment(ArtemisClient client, long participationId, boolean submit, ResultDTO result) throws ArtemisNetworkException {
        ArtemisRequest.put().path(List.of("participations", participationId, "manual-results")).param("submit", submit).body(result).execute(client);
    }

    public List<ResultDTO> nonAutomaticResults() {
        return this.results().stream().filter(r -> r.assessmentType() != AssessmentType.AUTOMATIC).toList();
    }

    private ProgrammingSubmissionDTO fetchLongFeedback(ArtemisClient client) throws ArtemisNetworkException {
        List<ResultDTO> results = new ArrayList<>(this.results().size());
        for (var result : this.results()) {
            // sometimes the feedbacks are not loaded, to fetch the long feedbacks, we need
            // to load the feedbacks first
            if (result.feedbacks() == null) {
                result = result.fetchFeedbacks(client, this.participation().id());
            }

            if (result.feedbacks() == null) {
                continue;
            }

            List<FeedbackDTO> cleanedFeedbacks = new ArrayList<>(result.feedbacks().size());
            for (var feedback : result.feedbacks()) {
                if (feedback == null) {
                    continue;
                }

                String detailText = feedback.detailText();
                if (feedback.hasLongFeedbackText()) {
                    detailText = FeedbackDTO.fetchLongFeedback(client, result.id(), feedback.id());
                }
                cleanedFeedbacks.add(new FeedbackDTO(detailText, feedback));
            }

            ResultDTO newResult = new ResultDTO(result.id(), result.completionDate(), result.successful(), result.score(), result.rated(), cleanedFeedbacks,
                    result.assessor(), result.assessmentType());
            results.add(newResult);
        }

        return new ProgrammingSubmissionDTO(this.id(), this.participation(), this.commitHash(), this.buildFailed(), results, this.submissionDate());
    }
}
