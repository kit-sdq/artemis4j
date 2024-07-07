/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j;

public class ArtemisNetworkException extends ArtemisClientException {
	public ArtemisNetworkException(String message) {
		super(message);
	}

	public ArtemisNetworkException(Throwable cause) {
		super(cause);
	}

	public ArtemisNetworkException(String message, Throwable cause) {
		super(message, cause);
	}
}
