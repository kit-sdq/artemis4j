/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.UserCreateDTO;
import edu.kit.kastel.sdq.artemis4j.grading.ArtemisConnection;
import edu.kit.kastel.sdq.artemis4j.grading.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for user management features (creation and deletion)
 */
class UserManagementTest {

    private static final String ADMIN_USER = System.getenv("ADMIN_USER");
    private static final String ADMIN_PASSWORD = System.getenv("ADMIN_PASSWORD");
    private static final String ARTEMIS_URL = System.getenv("ARTEMIS_URL");

    private static ArtemisConnection connection;
    private static boolean hasAdminPermissions = true;

    @BeforeAll
    static void setup() throws ArtemisClientException {
        Assumptions.assumeTrue(
                ARTEMIS_URL != null && !ARTEMIS_URL.isBlank(), "ARTEMIS_URL must be configured for integration tests");
        hasAdminPermissions = ADMIN_USER != null && ADMIN_PASSWORD != null;

        if (!hasAdminPermissions) {
            return;
        }

        ArtemisInstance artemisInstance = new ArtemisInstance(ARTEMIS_URL);
        connection = ArtemisConnection.connectWithUsernamePassword(artemisInstance, ADMIN_USER, ADMIN_PASSWORD);

        // Check if the user has admin permissions by trying to list users
        try {
            connection.getAllUsers();
        } catch (Exception e) {
            if (e.toString().contains("403")) {
                System.out.println("User does not have admin permissions for user management endpoints");
                hasAdminPermissions = false;
            } else {
                throw new ArtemisClientException(e);
            }
        }
    }

    private void skipIfNoAdminPermissions() {
        if (!hasAdminPermissions) {
            Assumptions.abort("User does not have admin permissions for user management endpoints");
        }
    }

    @Test
    void testCreateAndDeleteUser() throws ArtemisClientException {
        skipIfNoAdminPermissions();

        // Create a unique username to avoid conflicts
        String testUsername = "tu_" + UUID.randomUUID().toString().substring(0, 5);
        String testEmail = testUsername + "@example.com";
        String testFirstName = "Test";
        String testLastName = "User";
        String testPassword = "TempPassword123!";
        long createdUserId = -1;

        try {
            // Create the user
            UserCreateDTO userCreateDTO =
                    new UserCreateDTO(testUsername, testFirstName, testLastName, testEmail, testPassword, "en");

            User createdUser = connection.createUser(userCreateDTO);
            createdUserId = createdUser.getId();

            // Verify the user was created
            assertNotNull(createdUser);
            Assertions.assertEquals(testUsername, createdUser.getLogin());

            var foundById = connection.findUserById(createdUserId);
            assertTrue(foundById.isPresent(), "Created user should be resolvable by id");
            Assertions.assertEquals(testUsername, foundById.orElseThrow().getLogin());

            // The list of users seems to not reliably update right away or there are too many.
            // Let's at least check the API can list it if it does
            List<User> allUsers = connection.getAllUsers();
            boolean found = allUsers.stream().anyMatch(u -> u.getLogin().equals(testUsername));
            assertTrue(found, "Created user should appear in the list of all users at login=" + testUsername);

        } finally {
            try {
                connection.deleteUser(testUsername);

                // Verify the user was deleted
                List<User> allUsers = connection.getAllUsers();
                assertTrue(
                        allUsers.stream().noneMatch(u -> u.getLogin().equals(testUsername)),
                        "Deleted user should not appear in the list of all users");

                if (createdUserId > 0) {
                    assertTrue(
                            connection.findUserById(createdUserId).isEmpty(),
                            "Deleted user should not be resolvable by id");
                }

            } catch (Exception e) {
                System.out.println("Failed to delete user in finally block: " + e.getMessage());
            }
        }
    }

    @Test
    void testListAllUsers() throws ArtemisClientException {
        skipIfNoAdminPermissions();

        // Get all users
        List<User> allUsers = connection.getAllUsers();

        // Verify we can retrieve the list
        assertNotNull(allUsers);
        assertTrue(allUsers.size() > 0, "There should be at least one user in the system");

        // Verify the structure of users
        for (User user : allUsers) {
            assertNotNull(user.getLogin(), "User login should not be null");
        }
    }
}
