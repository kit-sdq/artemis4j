/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.*;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates methods to get data from and send data to Artemis
 */
public abstract class AbstractArtemisClient {
	private static final Logger log = LoggerFactory.getLogger(AbstractArtemisClient.class);
	private static final String DEFAULT_PROTOCOL_PREFIX = "https://";

	// paths
	protected static final String PROGRAMMING_SUBMISSIONS_PATHPART = "programming-submissions";
	protected static final String EXERCISES_PATHPART = "exercises";
	protected static final String COURSES_PATHPART = "courses";
	protected static final String EXAMS_PATHPART = "exams";
	protected static final String PARTICIPATIONS_PATHPART = "participations";
	protected static final String RESULTS_PATHPART = "results";
	protected static final String DETAILS_PATHPART = "details";

	protected static final String COOKIE_NAME_JWT = "jwt";

	protected static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	protected final String hostname;
	// e.g., https://
	private final String protocol;
	private final ObjectMapper orm;

	/**
	 * @param hostname the hostname of the artemis system. Will be transformed to
	 *                 domain-name:port
	 */
	protected AbstractArtemisClient(String hostname) {
		this.protocol = extractProtocol(hostname.trim());
		this.hostname = cleanupHostnameString(hostname.trim(), this.protocol);
		this.orm = this.createObjectMapper();
	}

	protected final OkHttpClient createClient(String token) {
		OkHttpClient.Builder builder = new OkHttpClient.Builder() //
				.connectTimeout(5, TimeUnit.SECONDS) //
				.callTimeout(20, TimeUnit.SECONDS)//
				.readTimeout(20, TimeUnit.SECONDS)//
				.writeTimeout(20, TimeUnit.SECONDS);

		if (token != null && !token.isBlank()) {
			builder.cookieJar(new CookieJar() {
				@Override
				public void saveFromResponse(@NotNull HttpUrl url, @NotNull List<Cookie> cookies) {
					// NOP
				}

				@NotNull
				@Override
				public List<Cookie> loadForRequest(@NotNull HttpUrl url) {
					return List.of(new Cookie.Builder().domain(AbstractArtemisClient.this.hostname).path("/").name(COOKIE_NAME_JWT).value(token).httpOnly()
							.secure().build());
				}
			});
		}
		return builder.build();
	}

	protected final <R> R call(OkHttpClient client, Request request, Class<R> resultClass) throws ArtemisClientException {
		try (Response response = client.newCall(request).execute()) {
			this.throwIfStatusUnsuccessful(response);
			if (resultClass == null) {
				return null;
			}
			return this.read(response.body().string(), resultClass);
		} catch (IOException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}
	}

	protected final HttpUrl path(Object... path) {
		StringBuilder requestPath = new StringBuilder(this.getApiRootURL());
		for (Object segment : path) {
			requestPath.append("/").append(segment);
		}
		return HttpUrl.parse(requestPath.toString());
	}

	protected final String getRootURL() {
		return this.protocol + this.hostname;
	}

	private static String cleanupHostnameString(String hostname, String protocol) {
		String finalHostname = hostname;
		if (hostname.startsWith(protocol)) {
			finalHostname = finalHostname.substring(protocol.length());
		}

		if (finalHostname.contains("/")) {
			finalHostname = finalHostname.split("/", 2)[0];
		}

		log.info("Using {} as hostname of artemis. Protocol is {}", finalHostname, protocol);
		return finalHostname;

	}

	private static String extractProtocol(String hostname) {
		if (!hostname.contains("://")) {
			return DEFAULT_PROTOCOL_PREFIX;
		}
		return hostname.split("://", 2)[0] + "://";
	}

	protected final String getApiRootURL() {
		return this.getRootURL() + "/api";
	}

	protected void throwIfStatusUnsuccessful(final Response response) throws ArtemisClientException {
		if (!response.isSuccessful()) {
			throw new ArtemisClientException("Got response code " + response.code() + " with message " + response.message());
		}
	}

	protected <E> String payload(E rspEntity) throws ArtemisClientException {
		try {
			return this.orm.writeValueAsString(rspEntity);
		} catch (JsonProcessingException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}
	}

	protected <E> E read(String rspEntity, Class<E> clazz) throws ArtemisClientException {
		if (clazz == String.class) {
			// noinspection unchecked
			return (E) rspEntity;
		}
		try {
			return this.orm.readValue(rspEntity, clazz);
		} catch (JsonProcessingException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}
	}

	private ObjectMapper createObjectMapper() {
		ObjectMapper oom = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).setSerializationInclusion(Include.NON_NULL);
		oom.setVisibility(oom.getSerializationConfig().getDefaultVisibilityChecker().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE).withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE).withCreatorVisibility(JsonAutoDetect.Visibility.ANY));
		return oom;
	}

}
