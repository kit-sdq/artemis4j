/* Licensed under EPL-2.0 2026. */
package edu.kit.kastel.sdq.artemis4j;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import org.junit.jupiter.api.Test;

public class TempTest {
    private static final String INSTRUCTOR_USER = System.getenv("INSTRUCTOR_USER");
    private static final String INSTRUCTOR_PASSWORD = System.getenv("INSTRUCTOR_PASSWORD");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");
    private static final String COURSE_ID = System.getenv("COURSE_ID");
    private static final String PROGRAMMING_EXERCISE_ID = System.getenv("PROGRAMMING_EXERCISE_ID");

    @Test
    public void testParticipation() throws ArtemisClientException {
        var connection = ArtemisConnection.connectWithUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), INSTRUCTOR_USER, INSTRUCTOR_PASSWORD);

        var course = connection.getCourseById(Long.parseLong(COURSE_ID));
        var exercise = course.getProgrammingExerciseById(Long.parseLong(PROGRAMMING_EXERCISE_ID));

        for (var participation : exercise.fetchAllParticipation()) {
            participation.fetchAllSubmissionsWithResults();
        }
    }
}
