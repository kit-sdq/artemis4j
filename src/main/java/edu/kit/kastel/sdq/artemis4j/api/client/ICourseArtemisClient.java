/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.Course;

import java.util.List;

/**
 * REST-Client to execute calls concerning courses.
 */
public interface ICourseArtemisClient {

	/**
	 * Returns all courses for current user. Needs extra rights to be called.
	 *
	 * @return all available courses, containing exercises and available submissions
	 * @throws ArtemisClientException if some errors occur while parsing the result.
	 */
	List<Course> getCoursesForAssessment() throws ArtemisClientException;
}
