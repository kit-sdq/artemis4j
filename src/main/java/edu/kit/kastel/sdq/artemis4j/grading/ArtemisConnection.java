/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.grading;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import edu.kit.kastel.sdq.artemis4j.LazyNetworkValue;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisClient;
import edu.kit.kastel.sdq.artemis4j.client.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.client.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.client.UserDTO;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Represents a connection to Artemis, holding the client and providing access
 * to the assessor (i.e. the user that you logged in with) and courses.
 */
public final class ArtemisConnection {
	private final ArtemisClient client;
	private final LazyNetworkValue<User> assessor;
	private final LazyNetworkValue<List<Course>> courses;
	private Locale locale = Locale.GERMANY;

	public static ArtemisConnection connectWithUsernamePassword(ArtemisInstance artemis, String username, String password) throws ArtemisNetworkException {
		return new ArtemisConnection(ArtemisClient.fromUsernamePassword(artemis, username, password));
	}

	public static ArtemisConnection fromToken(ArtemisInstance instance, String token) {
		return new ArtemisConnection(new ArtemisClient(instance, token, null));
	}

	private ArtemisConnection(ArtemisClient client) {
		this.client = client;
		this.assessor = new LazyNetworkValue<>(() -> new User(UserDTO.getAssessingUser(this.client)));
		this.courses = new LazyNetworkValue<>(() -> CourseDTO.fetchAll(this.client).stream().map(dto -> new Course(dto, this)).toList());
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public ArtemisClient getClient() {
		return client;
	}

	public User getAssessor() throws ArtemisNetworkException {
		return assessor.get();
	}

	public Locale getLocale() {
		return this.locale;
	}

	public List<Course> getCourses() throws ArtemisNetworkException {
		return Collections.unmodifiableList(courses.get());
	}
}
