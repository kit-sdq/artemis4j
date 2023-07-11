/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.api.client;

import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.util.Version;

import java.time.LocalDateTime;

public interface IUtilArtemisClient {
	/**
	 * Returns current time of server.
	 *
	 * @return current Date of server
	 */
	LocalDateTime getTime() throws ArtemisClientException;

	/**
	 * Get the current version of artemis.
	 *
	 * @return the current version of artemis
	 */
	Version getVersion() throws ArtemisClientException;
}
