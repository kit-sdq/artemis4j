/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import org.jspecify.annotations.Nullable;

public record ParticipationDTO(
        @JsonProperty long id,
        @JsonProperty @Nullable UserDTO student,
        @JsonProperty @Nullable String participantIdentifier,
        @JsonProperty @Nullable String userIndependentRepositoryUri,
        @JsonProperty @Nullable String repositoryUri,
        @JsonProperty @Nullable List<ResultDTO> results,
        @JsonProperty @Nullable List<ProgrammingSubmissionDTO> submissions) {

    public static ParticipationDTO startExercise(ArtemisClient client, long exerciseId) throws ArtemisNetworkException {
        return ArtemisRequest.post()
                .path(List.of("exercise", "exercises", exerciseId, "participations"))
                .executeAndDecode(client, ParticipationDTO.class);
    }

    public static boolean hasResult(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("programming", "programming-exercise-participations", participationId, "has-result"))
                .executeAndDecode(client, boolean.class);
    }

    public static void resetRepository(ArtemisClient client, long participationId, @Nullable Long gradedParticipationId)
            throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of(
                        "programming", "programming-exercise-participations", participationId, "reset-repository"))
                .param("gradedParticipationId", gradedParticipationId)
                .execute(client);
    }

    public static Optional<ParticipationDTO> getParticipationWithLatestResult(
            ArtemisClient client, long participationId) throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of(
                        "programming",
                        "programming-exercise-participations",
                        participationId,
                        "student-participation-with-latest-result-and-feedbacks"))
                .executeAndDecodeMaybe(client, ParticipationDTO.class);
    }

    public static ParticipationDTO getParticipationWithAllResults(ArtemisClient client, long participationId)
            throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of(
                        "programming",
                        "programming-exercise-participations",
                        participationId,
                        "student-participation-with-all-results"))
                .executeAndDecode(client, ParticipationDTO.class);
    }

    public static void delete(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        ArtemisRequest.delete()
                .path(List.of("exercise", "participations", participationId))
                .execute(client);
    }

    public static void cleanupBuildPlan(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        ArtemisRequest.put()
                .path(List.of("exercise", "participations", participationId, "cleanup-build-plan"))
                .execute(client);
    }

    public static String getVcsAccessToken(ArtemisClient client, long participationId) throws ArtemisNetworkException {
        // This token is scoped to one participation and is required for clone/push access.
        return ArtemisRequest.get()
                .path(List.of("core", "account", "participation-vcs-access-token"))
                .param("participationId", participationId)
                .executeAndDecode(client, String.class);
    }

    private static final int DEFAULT_PARTICIPATION_PAGE_SIZE = 100;

    public static List<ParticipationDTO> fetchForExercise(
            ArtemisClient client, long exerciseId, boolean withLatestResult) throws ArtemisNetworkException {
        List<ParticipationDTO> results = new ArrayList<>();

        for (int page = 0; ; page++) {
            List<ParticipationReferenceDTO> references = fetchParticipationPage(client, exerciseId, page);

            for (var reference : references) {
                ParticipationDTO participation = null;
                if (withLatestResult) {
                    participation = getParticipationWithLatestResult(client, reference.participationId())
                            .orElse(null);
                }

                if (participation == null) {
                    participation = getParticipationWithoutResults(client, reference.participationId());
                }

                results.add(participation);
            }

            // Technically the response contains a header to see if there are more pages
            // to fetch, but that would require changes to ArtemisClient and possibly exposing
            // okhttp code.
            //
            // The simpler solution is to check how many results were returned, and
            // if they are less than the page, there shouldn't be another page.
            if (references.size() < DEFAULT_PARTICIPATION_PAGE_SIZE) {
                return results;
            }
        }
    }

    private static ParticipationDTO getParticipationWithoutResults(ArtemisClient client, long participationId)
            throws ArtemisNetworkException {
        return ArtemisRequest.get()
                .path(List.of("exercise", "participations", participationId))
                .executeAndDecode(client, ParticipationDTO.class);
    }

    private static List<ParticipationReferenceDTO> fetchParticipationPage(
            ArtemisClient client, long exerciseId, int page) throws ArtemisNetworkException {
        return Arrays.asList(ArtemisRequest.get()
                .path(List.of("exercise", "exercises", exerciseId, "participations", "page"))
                .param("page", page)
                .param("pageSize", DEFAULT_PARTICIPATION_PAGE_SIZE)
                .param("sortingOrder", "ASCENDING")
                .param("sortedColumn", "participationId")
                .param("searchTerm", "")
                .executeAndDecode(client, ParticipationReferenceDTO[].class));
    }

    // This is to support pagination
    private record ParticipationReferenceDTO(@JsonProperty long participationId) {}
}
