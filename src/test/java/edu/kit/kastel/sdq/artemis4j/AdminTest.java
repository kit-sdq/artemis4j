/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseRole;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseBuildConfigCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.ProgrammingExerciseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.Course;
import edu.kit.kastel.sdq.artemis4j.grading.Participation;
import edu.kit.kastel.sdq.artemis4j.grading.ProgrammingExercise;
import edu.kit.kastel.sdq.artemis4j.grading.User;
import org.junit.jupiter.api.AfterAll;
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
    private User studentLogin;
    private String studentPassword;
    private User unassignedUser;
    private String unassignedUserPassword;
    private Course course;
    private ProgrammingExercise exercise;

    @BeforeAll
    void setup() throws ArtemisClientException {
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

        skipIfNoAdminPermissions();

        // Setup the course:

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String courseShortName = "a4j" + suffix;
        String courseTitle = "Artemis4J Test Course " + suffix;

        this.course = this.adminConnection.createCourse(
                CourseCreateDTO.minimal(courseTitle, courseShortName), null, null, null);

        assertTrue(this.course.getId() > 0L, "Expected a course id");
        assertEquals(courseTitle, this.course.getTitle());
        assertEquals(courseShortName, this.course.getShortName());

        // Setup the exercise:

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

        this.exercise = this.course.createProgrammingExercise(createExerciseDTO, true);

        assertTrue(this.exercise.getId() > 0L, "Expected an exercise id");
        assertEquals(exerciseShortName, this.exercise.getShortName());

        boolean exerciseVisible = this.course.getProgrammingExercises().stream()
                .anyMatch(exercise -> exercise.getId() == this.exercise.getId());
        assertTrue(exerciseVisible, "Created exercise should be visible in the course exercise list");
    }

    private void skipIfNoAdminPermissions() {
        if (!hasAdminPermissions) {
            Assumptions.abort("User does not have admin permissions for admin management endpoints");
        }
    }

    private UserCreateDTO randomUser(String suffix) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String login = "a4j_" + suffix + "_" + id;
        return new UserCreateDTO(login, "A4J", "Test", login + "@example.com", "TempPass123!", "en");
    }

    @BeforeEach
    void setupCourseExerciseAndStudent() throws ArtemisClientException {
        skipIfNoAdminPermissions();

        var studentCreateDTO = randomUser("student");
        var unassignedUserCreateDTO = randomUser("unassigned");

        this.studentLogin = this.adminConnection.createUser(studentCreateDTO);
        this.unassignedUser = this.adminConnection.createUser(unassignedUserCreateDTO);

        this.studentPassword = studentCreateDTO.password();
        this.unassignedUserPassword = unassignedUserCreateDTO.password();

        this.course.assignUser(this.studentLogin.getLogin(), CourseRole.STUDENT);
    }

    @AfterAll
    void teardown() throws ArtemisClientException {
        if (this.course == null) {
            return;
        }

        if (this.exercise != null) {
            this.course.deleteProgrammingExercise(this.exercise.getId());
            boolean stillVisible = this.course.getProgrammingExercises().stream()
                    .anyMatch(exercise -> exercise.getId() == this.exercise.getId());
            assertFalse(stillVisible, "Deleted exercise should not be visible in the course exercise list");
            this.exercise = null;
        }

        this.adminConnection.deleteCourse(this.course.getId());

        assertTrue(
                this.adminConnection.getCourses().stream().noneMatch(cour -> cour.getId() == this.course.getId()),
                "Expecting course to be deleted");
        this.course = null;
    }

    @AfterEach
    void cleanupCreatedResources() throws ArtemisClientException {
        for (var user : List.of(this.unassignedUser, this.studentLogin)) {
            if (user == null) {
                continue;
            }

            this.adminConnection.deleteUser(user.getLogin());
            assertTrue(
                    this.adminConnection.findUserByLogin(user.getLogin()).isEmpty(),
                    "Deleted user should not be resolvable by id");
        }
        this.unassignedUser = null;
        this.studentLogin = null;
    }

    @Test
    void testAdminCanAssignStudentToCourse() throws ArtemisClientException {
        assertTrue(
                this.course.fetchAllStudents().stream()
                        .noneMatch(u -> u.getLogin().equals(this.unassignedUser.getLogin())),
                "Unassigned user should not be assigned to the course yet");

        this.course.assignUser(this.unassignedUser.getLogin(), CourseRole.STUDENT);

        assertTrue(
                this.course.fetchAllStudents().stream()
                        .anyMatch(u -> u.getLogin().equals(this.unassignedUser.getLogin())),
                "Unassigned user should appear in the course student list");
    }

    @Test
    void testStudentCanListAndSelfEnrollInCourse() throws ArtemisClientException {
        ArtemisConnection studentConnection = ArtemisConnection.connectWithUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), this.unassignedUser.getLogin(), this.unassignedUserPassword);

        List<Course> enrollable = studentConnection.getCoursesForEnrollment();
        Assumptions.assumeFalse(enrollable.isEmpty(), "No self-enrollable courses available on this Artemis instance");

        Course target = enrollable.getFirst();
        target.enrollSelf();

        List<UserDTO> students = CourseDTO.fetchAllStudents(adminConnection.getClient(), target.getId());
        boolean enrolled = students.stream().anyMatch(u -> u.login().equals(this.unassignedUser.getLogin()));
        assertTrue(enrolled, "Self-enrolled student should appear in the course student list");
    }

    @Test
    void testStudentCanStartExerciseAndReceiveRepositoryAccess() throws ArtemisClientException {
        ArtemisClient studentClient = ArtemisClient.fromUsernamePassword(
                new ArtemisInstance(ARTEMIS_URL), this.studentLogin.getLogin(), this.studentPassword);
        ArtemisConnection studentConnection = new ArtemisConnection(studentClient);

        var studentCourse = studentConnection.getCourseById(this.course.getId());
        var studentExercise = studentCourse.getProgrammingExerciseById(this.exercise.getId());

        Participation participation = studentExercise.startParticipation();
        String repositoryUrl = participation.getUserIndependentRepositoryUri().orElseThrow();

        UserDTO.createVCSToken(ZonedDateTime.now().plusDays(7), studentClient);
        User user = studentConnection.refreshAssessor();

        assertNotNull(participation);
        assertTrue(participation.getId() > 0L, "Participation id should be positive");
        assertFalse(repositoryUrl.isBlank(), "Repository URL should not be blank");
        assertTrue(user.getVcsAccessToken().isPresent(), "VCS token should exist after creation");
        assertFalse(user.getVcsAccessToken().get().isBlank(), "VCS token should not be blank");
    }

    @Test
    void testUserCanFindThemselves() throws ArtemisClientException {
        var assessor = this.adminConnection.getAssessor();
        var resolved = this.adminConnection.findUserByLogin(assessor.getLogin());

        assertTrue(resolved.isPresent());
        assertEquals(assessor.getId(), resolved.orElseThrow().getId());
        assertEquals(assessor.getLogin(), resolved.orElseThrow().getLogin());
    }

    @Test
    void testAdminCanListAllUsers() throws ArtemisClientException {
        List<User> allUsers = this.adminConnection.getAllUsers();

        assertNotNull(allUsers);
        assertFalse(allUsers.isEmpty(), "There should be at least one user in the system");
        for (User user : allUsers) {
            assertNotNull(user.getLogin(), "User login should not be null");
        }
    }
}
