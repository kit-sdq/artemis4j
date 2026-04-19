/* Licensed under EPL-2.0 2024-2026. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ManagementInfoDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserCreateDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserSshPublicKeyDTO;

/**
 * Represents a connection to Artemis, holding the client and providing access
 * to the assessor (i.e. the user that you logged in with) and courses.
 */
public final class ArtemisConnection {
    private final ArtemisClient client;
    private final LazyNetworkValue<ManagementInfoDTO> managementInfo;
    private final LazyNetworkValue<User> assessor;
    private final LazyNetworkValue<List<Course>> courses;

    public static ArtemisConnection connectWithUsernamePassword(
            ArtemisInstance artemis, String username, String password) throws ArtemisNetworkException {
        return new ArtemisConnection(ArtemisClient.fromUsernamePassword(artemis, username, password));
    }

    public static ArtemisConnection fromToken(ArtemisInstance instance, String token) {
        return new ArtemisConnection(new ArtemisClient(instance, token, null));
    }

    public ArtemisConnection(ArtemisClient client) {
        this.client = client;
        this.managementInfo = new LazyNetworkValue<>(() -> ManagementInfoDTO.fetch(this.client));
        this.assessor = new LazyNetworkValue<>(() -> new User(UserDTO.getAssessingUser(this.client)));
        this.courses = new LazyNetworkValue<>(() ->
                this.fetchCourses().stream().map(dto -> new Course(dto, this)).toList());
    }

    private List<CourseDTO> fetchCourses() throws ArtemisNetworkException {
        try {
            return CourseDTO.fetchAll(this.client);
        } catch (ArtemisNetworkException exception) {
            // This call might fail if the user is a student, in that case build the courses from the dashboard
            // endpoint:
            return CourseDTO.fetchForDashboard(this.client);
        }
    }

    public ArtemisClient getClient() {
        return client;
    }

    public ManagementInfoDTO getManagementInfo() throws ArtemisNetworkException {
        return managementInfo.get();
    }

    public User getAssessor() throws ArtemisNetworkException {
        return assessor.get();
    }

    public User refreshAssessor() throws ArtemisNetworkException {
        assessor.invalidate();
        return getAssessor();
    }

    public List<Course> getCourses() throws ArtemisNetworkException {
        return Collections.unmodifiableList(courses.get());
    }

    public List<Course> getCoursesForEnrollment() throws ArtemisNetworkException {
        return CourseDTO.fetchForEnrollment(this.client).stream()
                .map(dto -> new Course(dto, this))
                .toList();
    }

    public Course getCourseById(long id) throws ArtemisNetworkException {
        return courses.get().stream()
                .filter(c -> c.getId() == id)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No course with id " + id + " found"));
    }

    /**
     * Retrieves all users. Note that this requires admin permissions.
     * @return A list of all users.
     */
    public List<User> getAllUsers() throws ArtemisNetworkException {
        return UserDTO.getAllUsers(this.client).stream().map(User::new).toList();
    }

    public List<UserSshPublicKey> listAssessorPublicKeys() throws ArtemisNetworkException {
        return UserSshPublicKeyDTO.getCurrentUserSshKeys(this.client).stream()
                .map(dto -> new UserSshPublicKey(dto, this))
                .toList();
    }

    /**
     * Finds a user based on their login name.
     *
     * @param login the login name, must not be empty, null and should be at least 3 characters long.
     *              It will only return a result if an exact match has been found.
     * @return the user instance or empty if it could not be found.
     * @throws ArtemisNetworkException if the login name is not a valid search term, or it failed to make the request
     */
    public Optional<User> findUserByLogin(String login) throws ArtemisNetworkException {
        var currentUser = this.getAssessor();
        if (login.equals(currentUser.getLogin())) {
            return Optional.of(currentUser);
        }

        // This requires at least student privileges in the relevant courses:
        for (var course : this.getCourses()) {
            if (course.getRoles(login).isEmpty()) {
                continue;
            }

            var user = course.findUserByLogin(login)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "User with login %s not found in course %d, but has role in the course"
                                    .formatted(login, course.getId())));
            return Optional.of(user);
        }

        return this.getAllUsers().stream()
                .filter(user -> login.equals(user.getLogin()))
                .findFirst();
    }

    /**
     * Creates a new user. Note that this requires admin permissions.
     * @param userCreateDTO The data for the new user.
     * @return The created user.
     */
    public User createUser(UserCreateDTO userCreateDTO) throws ArtemisNetworkException {
        return new User(UserDTO.createUser(this.client, userCreateDTO));
    }

    /**
     * Creates a course.
     * <p>
     * Note that this requires admin permissions.
     */
    public Course createCourse(CourseCreateDTO courseCreateDTO) throws ArtemisNetworkException {
        CourseDTO created = CourseDTO.createCourse(this.client, courseCreateDTO);
        if (created == null) {
            throw new IllegalStateException("Failed to create course for " + courseCreateDTO);
        }

        this.courses.invalidate();
        return new Course(created, this);
    }

    /**
     * Deletes a course. Note that this requires admin permissions.
     */
    public void deleteCourse(long courseId) throws ArtemisNetworkException {
        CourseDTO.deleteCourse(this.client, courseId);
        this.courses.invalidate();
    }

    /**
     * Deletes a user by their login. Note that this requires admin permissions.
     * @param username The login of the user to delete.
     */
    public void deleteUser(String username) throws ArtemisNetworkException {
        UserDTO.deleteUser(this.client, username);
    }
}
