/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseBuildConfigCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CourseAndProgrammingExerciseManagementTest {

    private static final String ADMIN_USER = System.getenv("ADMIN_USER");
    private static final String ADMIN_PASSWORD = System.getenv("ADMIN_PASSWORD");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");

    private static ArtemisConnection connection;
    private static boolean hasAdminPermissions = true;

    @BeforeAll
    static void setup() throws ArtemisClientException {
        Assumptions.assumeTrue(ARTEMIS_URL != null && !ARTEMIS_URL.isBlank(), "ARTEMIS_URL must be configured");
        hasAdminPermissions =
                ADMIN_USER != null && !ADMIN_USER.isBlank() && ADMIN_PASSWORD != null && !ADMIN_PASSWORD.isBlank();

        if (!hasAdminPermissions) {
            return;
        }

        connection = ArtemisConnection.connectWithUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), ADMIN_USER, ADMIN_PASSWORD);
        try {
            connection.getAllUsers();
        } catch (Exception e) {
            if (e.toString().contains("403")) {
                hasAdminPermissions = false;
            } else {
                throw new ArtemisClientException(e);
            }
        }
    }

    @Test
    void createAndDeleteCourseAndProgrammingExerciseWithAdvancedBuildOptions() throws ArtemisClientException {
        if (!hasAdminPermissions) {
            Assumptions.abort("User does not have admin permissions for course management endpoints");
        }

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String courseShortName = "a4j" + suffix;
        String courseTitle = "Artemis4J Test Course " + suffix;

        long createdCourseId = -1;
        long createdExerciseId = -1;

        try {
            Course createdCourse = connection.createCourse(CourseCreateDTO.minimal(courseTitle, courseShortName));
            createdCourseId = createdCourse.getId();

            assertTrue(createdCourseId > 0L, "Expected a persisted course id");
            assertEquals(courseTitle, createdCourse.getTitle());

            var advancedBuildConfig = new ProgrammingExerciseBuildConfigCreateDTO(
                    true,
                    null,
                    "version: 2\nplan:\n  project-key: PLACEHOLDER\n",
                    "#!/usr/bin/env bash\nset -e\necho 'running advanced build options'\n",
                    false,
                    "test",
                    "assignment",
                    "solution",
                    180,
                    "{\"memory\":512,\"cpuCount\":1}",
                    null,
                    true,
                    "main|release/.*");

            String exerciseShortName = "adv" + suffix;
            var createExerciseDTO = ProgrammingExerciseCreateDTO.minimalCourseExercise(
                    "Advanced Build Options " + suffix, exerciseShortName, "edu.kit.kastel", advancedBuildConfig);

            ProgrammingExercise createdExercise = createdCourse.createProgrammingExercise(createExerciseDTO, true);
            createdExerciseId = createdExercise.getId();
            long persistedExerciseId = createdExerciseId;

            assertTrue(createdExerciseId > 0, "Expected a persisted exercise id");
            assertEquals(exerciseShortName, createdExercise.getShortName());

            boolean exerciseVisible = createdCourse.getProgrammingExercises().stream()
                    .anyMatch(exercise -> exercise.getId() == persistedExerciseId);
            assertTrue(exerciseVisible, "Created exercise should be visible in the course exercise list");

            createdCourse.deleteProgrammingExercise(createdExerciseId);
            createdExerciseId = -1;

            boolean stillVisible = createdCourse.getProgrammingExercises().stream()
                    .anyMatch(exercise -> exercise.getId() == createdExercise.getId());
            assertFalse(stillVisible, "Deleted exercise should not be visible in the course exercise list");

        } finally {
            if (createdExerciseId > 0 && createdCourseId > 0) {
                try {
                    Course course = connection.getCourseById((int) createdCourseId);
                    course.deleteProgrammingExercise(createdExerciseId);
                } catch (Exception ignored) {
                    // Best effort cleanup
                }
            }

            if (createdCourseId > 0) {
                try {
                    connection.deleteCourse(createdCourseId);
                } catch (Exception ignored) {
                    // Best effort cleanup
                }
            }
        }
    }
}
