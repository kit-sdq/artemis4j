/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.ParticipationDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingSubmissionDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import org.junit.jupiter.api.Test;

class ProgrammingExerciseLatestResultTest {
    private static final String DEFAULT_ARTEMIS_URL = "http://127.0.0.1:8080";
    private static final String DEFAULT_USERNAME = "artemis";
    private static final String DEFAULT_PASSWORD = "artemis123";

    @Test
    void fetchLatestResultForParticipationReturnsEmptyWhenArtemisRespondsWithEmptyBody()
            throws ArtemisNetworkException {
        var artemisUrl = envOrDefault("ARTEMIS_URL", DEFAULT_ARTEMIS_URL);
        var username = envOrDefault("ARTEMIS_USERNAME", DEFAULT_USERNAME);
        var password = envOrDefault("ARTEMIS_PASSWORD", DEFAULT_PASSWORD);

        var connection =
                ArtemisConnection.connectWithUsernamePassword(new ArtemisInstance(artemisUrl), username, password);
        var participation = findParticipationWithoutLatestResult(connection);

        assertTrue(
                participation.isPresent(),
                "Expected a participation without a latest result on the configured Artemis instance");

        var latestResult = assertDoesNotThrow(() -> ProgrammingSubmissionDTO.fetchLatestWithFeedbacksForParticipation(
                connection.getClient(), participation.orElseThrow().id()));

        assertTrue(latestResult.isEmpty(), "Expected Optional.empty() for Artemis' empty response body");
    }

    private static Optional<ParticipationDTO> findParticipationWithoutLatestResult(ArtemisConnection connection)
            throws ArtemisNetworkException {
        for (Course course : connection.getCourses()) {
            for (ProgrammingExercise exercise : course.getProgrammingExercises()) {
                for (ParticipationDTO participation :
                        ParticipationDTO.fetchForExercise(connection.getClient(), exercise.getId(), true)) {
                    if (participation.student() != null
                            && (participation.results() == null
                                    || participation.results().isEmpty())) {
                        return Optional.of(participation);
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static String envOrDefault(String key, String fallback) {
        var value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
