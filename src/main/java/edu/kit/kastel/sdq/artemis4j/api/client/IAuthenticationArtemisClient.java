/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.User;

/**
 * REST-Client to execute calls concerning login and authentication.
 */
public interface IAuthenticationArtemisClient {
	String getArtemisUrl();

	/**
	 * Returns raw token as String. The token can be used to authenticate for
	 * REST-calls.
	 *
	 * @return security token
	 */
	String getToken();

	/**
	 * @return the Artemis {@code Assessor} object (needed for submitting the
	 *         assessment).
	 */
	User getUser();

	/**
	 * Login to Artemis. Must be called before any other authorized call.
	 *
	 * @throws ArtemisClientException if some errors occur while parsing the result
	 *                                or if authentication fails.
	 */
	void login() throws ArtemisClientException;

	/**
	 * Checks whether the client is logged in.
	 * 
	 * @return true if logged in, false otherwise.
	 */
	boolean isLoggedIn();
}
