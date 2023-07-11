/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.client.IUtilArtemisClient;
import edu.kit.kastel.sdq.artemis4j.util.Version;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public class UtilArtemisClient extends AbstractArtemisClient implements IUtilArtemisClient {
	private final OkHttpClient client;

	public UtilArtemisClient(final String hostname) {
		super(hostname);
		this.client = this.createClient(null);
	}

	@Override
	public LocalDateTime getTime() throws ArtemisClientException {
		Request request = new Request.Builder().url(this.path("public", "time")).get().build();
		return this.call(this.client, request, LocalDateTime.class);
	}

	@Override
	public Version getVersion() throws ArtemisClientException {
		Request request = new Request.Builder().url(this.getRootURL() + "/management/info").get().build();
		var info = this.call(this.client, request, Info.class);
		return Version.fromString(info.build.version);
	}

	private static class Info {
		@JsonProperty
		private Build build;

		private static class Build {
			@JsonProperty
			private String version;
		}
	}
}
