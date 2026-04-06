/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseRole;
import edu.kit.kastel.sdq.artemis4j.client.ParticipationDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseBuildConfigCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.Participation;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdminTest {
    private static final String ADMIN_USER = System.getenv("ADMIN_USER");
    private static final String ADMIN_PASSWORD = System.getenv("ADMIN_PASSWORD");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");

    private ArtemisConnection adminConnection;
    private boolean hasAdminPermissions = true;
    private UserCreateDTO preparedUserData;
    private String login;
    private long cleanupCreatedUserId = -1;
    private long cleanupCourseId = -1;
    private long cleanupExerciseId = -1;

    @BeforeAll
    void setup() {
        Assumptions.assumeTrue(
                ARTEMIS_URL != null && !ARTEMIS_URL.isBlank(), "ARTEMIS_URL must be configured for integration tests");
        hasAdminPermissions =
                ADMIN_USER != null && !ADMIN_USER.isBlank() && ADMIN_PASSWORD != null && !ADMIN_PASSWORD.isBlank();
        Assumptions.assumeTrue(
                hasAdminPermissions, "ADMIN_USER and ADMIN_PASSWORD must be configured for integration tests");

        try {
            adminConnection = ArtemisConnection.connectWithUsernamePassword(
                    new ArtemisInstance(ARTEMIS_URL), ADMIN_USER, ADMIN_PASSWORD);
            try {
                adminConnection.getAllUsers();
            } catch (Exception e) {
                if (e.toString().contains("403")) {
                    hasAdminPermissions = false;
                } else {
                    throw new ArtemisClientException(e);
                }
            }
        } catch (Exception ex) {
            Assumptions.abort(
                    "Skipping integration tests: cannot connect to Artemis admin account: " + ex.getMessage());
        }
    }

    private void skipIfNoAdminPermissions() {
        if (!hasAdminPermissions) {
            Assumptions.abort("User does not have admin permissions for admin management endpoints");
        }
    }

    @BeforeEach
    void resetCleanupState() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        login = null;
        cleanupCreatedUserId = -1;
        cleanupCourseId = -1;
        cleanupExerciseId = -1;

        preparedUserData = randomUser("setup");
        login = preparedUserData.login();
        User createdUser = adminConnection.createUser(preparedUserData);
        cleanupCreatedUserId = createdUser.getId();
    }

    @AfterEach
    void cleanupCreatedResources() throws ArtemisClientException {
        if (cleanupCourseId > 0L && cleanupExerciseId > 0L) {
            Course course = adminConnection.getCourseById((int) cleanupCourseId);
            course.deleteProgrammingExercise(cleanupExerciseId);
        }

        if (cleanupCourseId > 0) {
            adminConnection.deleteCourse(cleanupCourseId);
        }

        if (login != null && !login.isBlank()) {
            adminConnection.deleteUser(login);
            if (cleanupCreatedUserId > 0) {
                assertTrue(
                        adminConnection.findUserById(cleanupCreatedUserId).isEmpty(),
                        "Deleted user should not be resolvable by id");
            }
        }
    }

    @Test
    void testAdminCanAssignStudentToCourse() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        Course course = pickAnyCourseWithProgrammingExercise(adminConnection)
                .orElseThrow(() -> new ArtemisClientException("No course with programming exercise available."));

        course.assignUser(login, CourseRole.STUDENT);

        List<UserDTO> students = CourseDTO.fetchAllStudents(adminConnection.getClient(), course.getId());
        boolean assigned = students.stream().anyMatch(u -> u.login().equals(login));
        assertTrue(assigned, "Assigned student should appear in the course student list");
    }

    @Test
    void testStudentCanListAndSelfEnrollInCourse() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        ArtemisConnection studentConnection = ArtemisConnection.connectWithUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), login, preparedUserData.password());

        List<Course> enrollable = studentConnection.getCoursesForEnrollment();
        Assumptions.assumeFalse(enrollable.isEmpty(), "No self-enrollable courses available on this Artemis instance");

        Course target = enrollable.getFirst();
        target.enrollSelf();

        List<UserDTO> students = CourseDTO.fetchAllStudents(adminConnection.getClient(), target.getId());
        boolean enrolled = students.stream().anyMatch(u -> u.login().equals(login));
        assertTrue(enrolled, "Self-enrolled student should appear in the course student list");
    }

    @Test
    void testStudentCanStartExerciseAndReceiveRepositoryAccess() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        Course course = pickAnyCourseWithProgrammingExercise(adminConnection)
                .orElseThrow(() -> new ArtemisClientException("No course with programming exercise available."));
        course.assignUser(login, CourseRole.STUDENT);

        ArtemisClient studentClient = ArtemisClient.fromUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), login, preparedUserData.password());
        ArtemisConnection studentConnection = new ArtemisConnection(studentClient);

        Participation participation = null;

        for (var exercise : course.getProgrammingExercises()) {
            try {
                participation = new Participation(
                        ParticipationDTO.startExercise(studentClient, exercise.getId()), studentConnection);
                break;
            } catch (ArtemisNetworkException ignored) {
                // Try the next exercise. Some exercises cannot be started due to date/state restrictions.
            }
        }

        Assumptions.assumeTrue(
                participation != null, "Could not start any programming exercise in the selected course");
        String repositoryUrl = participation.getRepositoryUrl().orElseThrow();

        UserDTO user = UserDTO.getAssessingUser(studentClient);
        if (user.vcsAccessToken() == null || user.vcsAccessToken().isBlank()) {
            UserDTO.createVCSToken(ZonedDateTime.now().plusDays(7), studentClient);
            user = UserDTO.getAssessingUser(studentClient);
        }

        assertNotNull(participation);
        assertTrue(participation.getId() > 0, "Participation id should be positive");
        assertFalse(repositoryUrl.isBlank(), "Repository URL should not be blank");
        assertNotNull(user.vcsAccessToken(), "VCS token should exist after creation");
        assertFalse(user.vcsAccessToken().isBlank(), "VCS token should not be blank");
    }

    @Test
    void testStudentCanFetchParticipationVcsAccessToken() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        Course course = pickAnyCourseWithProgrammingExercise(adminConnection)
                .orElseThrow(() -> new ArtemisClientException("No course with programming exercise available."));
        course.assignUser(login, CourseRole.STUDENT);

        ArtemisClient studentClient = ArtemisClient.fromUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), login, preparedUserData.password());
        ArtemisConnection studentConnection = new ArtemisConnection(studentClient);

        Participation participation = null;
        for (var exercise : course.getProgrammingExercises()) {
            try {
                participation = new Participation(
                        ParticipationDTO.startExercise(studentClient, exercise.getId()), studentConnection);
                break;
            } catch (ArtemisNetworkException ignored) {
                // Try the next exercise. Some exercises cannot be started due to date/state restrictions.
            }
        }

        Assumptions.assumeTrue(
                participation != null, "Could not start any programming exercise in the selected course");

        String participationToken = participation.getVcsAccessToken();

        assertNotNull(participationToken, "Participation VCS access token should not be null");
        assertFalse(participationToken.isBlank(), "Participation VCS access token should not be blank");
    }

    @Test
    void testAdminFindUserByIdReturnsCurrentUser() throws ArtemisClientException {
        skipIfNoAdminPermissions();
        var assessor = this.adminConnection.getAssessor();
        var resolved = this.adminConnection.findUserById(assessor.getId());

        assertTrue(resolved.isPresent());
        assertEquals(assessor.getId(), resolved.orElseThrow().getId());
        assertEquals(assessor.getLogin(), resolved.orElseThrow().getLogin());
    }

    @Test
    void testAdminCanCreateAndDeleteUser() throws ArtemisClientException {
        skipIfNoAdminPermissions();

        String login = this.login;
        User createdUser = adminConnection.findUserById(cleanupCreatedUserId).orElseThrow();

        assertNotNull(createdUser);
        assertEquals(login, createdUser.getLogin());

        var foundById = adminConnection.findUserById(cleanupCreatedUserId);
        assertTrue(foundById.isPresent(), "Created user should be resolvable by id");
        assertEquals(login, foundById.orElseThrow().getLogin());

        List<User> allUsers = adminConnection.getAllUsers();
        boolean found = allUsers.stream().anyMatch(u -> u.getLogin().equals(login));
        assertTrue(found, "Created user should appear in the list of all users");
    }

    @Test
    void testAdminCanListAllUsers() throws ArtemisClientException {
        skipIfNoAdminPermissions();

        List<User> allUsers = adminConnection.getAllUsers();

        assertNotNull(allUsers);
        assertFalse(allUsers.isEmpty(), "There should be at least one user in the system");
        for (User user : allUsers) {
            assertNotNull(user.getLogin(), "User login should not be null");
        }
    }

    @Test
    void testAdminCanCreateAndDeleteCourseAndProgrammingExerciseWithAdvancedBuildOptions()
            throws ArtemisClientException {
        skipIfNoAdminPermissions();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String courseShortName = "a4j" + suffix;
        String courseTitle = "Artemis4J Test Course " + suffix;

        Course createdCourse = adminConnection.createCourse(CourseCreateDTO.minimal(courseTitle, courseShortName));
        cleanupCourseId = createdCourse.getId();

        assertTrue(cleanupCourseId > 0L, "Expected a persisted course id");
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
        cleanupExerciseId = createdExercise.getId();
        long persistedExerciseId = cleanupExerciseId;

        assertTrue(cleanupExerciseId > 0, "Expected a persisted exercise id");
        assertEquals(exerciseShortName, createdExercise.getShortName());

        boolean exerciseVisible = createdCourse.getProgrammingExercises().stream()
                .anyMatch(exercise -> exercise.getId() == persistedExerciseId);
        assertTrue(exerciseVisible, "Created exercise should be visible in the course exercise list");

        createdCourse.deleteProgrammingExercise(cleanupExerciseId);
        cleanupExerciseId = -1;

        boolean stillVisible = createdCourse.getProgrammingExercises().stream()
                .anyMatch(exercise -> exercise.getId() == createdExercise.getId());
        assertFalse(stillVisible, "Deleted exercise should not be visible in the course exercise list");
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
}
