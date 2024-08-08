/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import java.util.Collections;
import java.util.List;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.ManagementInfoDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;

/**
 * Represents a connection to Artemis, holding the client and providing access
 * to the assessor (i.e. the user that you logged in with) and courses.
 */
public final class ArtemisConnection {
    private final ArtemisClient client;
    private final LazyNetworkValue<ManagementInfoDTO> managementInfo;
    private final LazyNetworkValue<User> assessor;
    private final LazyNetworkValue<List<Course>> courses;

    public static ArtemisConnection connectWithUsernamePassword(ArtemisInstance artemis, String username, String password) throws ArtemisNetworkException {
        return new ArtemisConnection(ArtemisClient.fromUsernamePassword(artemis, username, password));
    }

    public static ArtemisConnection fromToken(ArtemisInstance instance, String token) {
        return new ArtemisConnection(new ArtemisClient(instance, token, null));
    }

    public ArtemisConnection(ArtemisClient client) {
        this.client = client;
        this.managementInfo = new LazyNetworkValue<>(() -> ManagementInfoDTO.fetch(this.client));
        this.assessor = new LazyNetworkValue<>(() -> new User(UserDTO.getAssessingUser(this.client)));
        this.courses = new LazyNetworkValue<>(() -> CourseDTO.fetchAll(this.client).stream().map(dto -> new Course(dto, this)).toList());
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

    public List<Course> getCourses() throws ArtemisNetworkException {
        return Collections.unmodifiableList(courses.get());
    }

    public Course getCourseById(int id) throws ArtemisNetworkException {
        return courses.get().stream().filter(c -> c.getId() == id).findAny()
                .orElseThrow(() -> new IllegalArgumentException("No course with id " + id + " found"));
    }
}
