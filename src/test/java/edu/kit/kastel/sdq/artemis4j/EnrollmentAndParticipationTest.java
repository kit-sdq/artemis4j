/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseRole;
import edu.kit.kastel.sdq.artemis4j.client.ParticipationDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EnrollmentAndParticipationTest {
    private static final String ADMIN_USER = System.getenv("ADMIN_USER");
    private static final String ADMIN_PASSWORD = System.getenv("ADMIN_PASSWORD");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");

    private ArtemisConnection adminConnection;

    @BeforeAll
    void setup() {
        try {
            adminConnection = ArtemisConnection.connectWithUsernamePassword(
                    new ArtemisInstance(ARTEMIS_URL), ADMIN_USER, ADMIN_PASSWORD);
        } catch (Exception ex) {
            Assumptions.abort(
                    "Skipping integration tests: cannot connect to Artemis admin account: " + ex.getMessage());
        }
    }

    @Test
    void testAdminCanAssignStudentToCourse() throws ArtemisClientException {
        UserCreateDTO userData = randomUser("assign");
        String login = userData.login();

        Course course = pickAnyCourseWithProgrammingExercise(adminConnection)
                .orElseThrow(() -> new ArtemisClientException("No course with programming exercise available."));

        try {
            adminConnection.createUser(userData);
            course.assignUser(login, CourseRole.STUDENT);

            List<UserDTO> students = CourseDTO.fetchAllStudents(adminConnection.getClient(), course.getId());
            boolean assigned = students.stream().anyMatch(u -> u.login().equals(login));
            assertTrue(assigned, "Assigned student should appear in the course student list");
        } finally {
            cleanupUser(login);
        }
    }

    @Test
    void testStudentCanListAndSelfEnrollInCourse() throws ArtemisClientException {
        UserCreateDTO userData = randomUser("enroll");
        String login = userData.login();

        try {
            adminConnection.createUser(userData);
            ArtemisConnection studentConnection = ArtemisConnection.connectWithUsernamePassword(
                    new ArtemisInstance(ARTEMIS_URL), login, userData.password());

            List<Course> enrollable = studentConnection.getCoursesForEnrollment();
            Assumptions.assumeFalse(
                    enrollable.isEmpty(), "No self-enrollable courses available on this Artemis instance");

            Course target = enrollable.getFirst();
            target.enrollSelf();

            List<UserDTO> students = CourseDTO.fetchAllStudents(adminConnection.getClient(), target.getId());
            boolean enrolled = students.stream().anyMatch(u -> u.login().equals(login));
            assertTrue(enrolled, "Self-enrolled student should appear in the course student list");
        } finally {
            cleanupUser(login);
        }
    }

    @Test
    void testStudentCanStartExerciseAndReceiveRepositoryAccess() throws ArtemisClientException {
        UserCreateDTO userData = randomUser("start");
        String login = userData.login();

        try {
            adminConnection.createUser(userData);

            Course course = pickAnyCourseWithProgrammingExercise(adminConnection)
                    .orElseThrow(() -> new ArtemisClientException("No course with programming exercise available."));
            course.assignUser(login, CourseRole.STUDENT);

            ArtemisClient studentClient =
                    ArtemisClient.fromUsernamePassword(new ArtemisInstance(ARTEMIS_URL), login, userData.password());

            ParticipationDTO participation = null;

            for (var exercise : course.getProgrammingExercises()) {
                try {
                    participation = exercise.startParticipation();
                    break;
                } catch (ArtemisNetworkException ignored) {
                    // Try the next exercise. Some exercises cannot be started due to date/state restrictions.
                }
            }

            Assumptions.assumeTrue(
                    participation != null, "Could not start any programming exercise in the selected course");
            String repositoryUrl = participation.repositoryUrl().orElseThrow();

            UserDTO user = UserDTO.getAssessingUser(studentClient);
            if (user.vcsAccessToken() == null || user.vcsAccessToken().isBlank()) {
                UserDTO.createVCSToken(ZonedDateTime.now().plusDays(7), studentClient);
                user = UserDTO.getAssessingUser(studentClient);
            }

            assertNotNull(participation);
            assertTrue(participation.id() > 0, "Participation id should be positive");
            assertFalse(repositoryUrl.isBlank(), "Repository URL should not be blank");
            assertNotNull(user.vcsAccessToken(), "VCS token should exist after creation");
            assertFalse(user.vcsAccessToken().isBlank(), "VCS token should not be blank");
        } finally {
            cleanupUser(login);
        }
    }

    private Optional<Course> pickAnyCourseWithProgrammingExercise(ArtemisConnection connection)
            throws ArtemisClientException {
        for (Course course : connection.getCourses()) {
            if (!course.getProgrammingExercises().isEmpty()) {
                return Optional.of(course);
            }
        }
        return Optional.empty();
    }

    private UserCreateDTO randomUser(String suffix) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String login = "a4j_" + suffix + "_" + id;
        return new UserCreateDTO(login, "A4J", "Test", login + "@example.com", "TempPass123!", "en");
    }

    private void cleanupUser(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        try {
            adminConnection.deleteUser(login);
        } catch (Exception ignored) {
            // Best-effort cleanup for test users.
        }
    }
}
