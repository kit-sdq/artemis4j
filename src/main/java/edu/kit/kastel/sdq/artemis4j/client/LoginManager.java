/* Licensed under EPL-2.0 2022-2023. */
package edu.kit.kastel.sdq.artemis4j.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import edu.kit.kastel.sdq.artemis4j.api.ArtemisClientException;
import edu.kit.kastel.sdq.artemis4j.api.artemis.User;
import edu.kit.kastel.sdq.artemis4j.api.client.IAuthenticationArtemisClient;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

public class LoginManager extends AbstractArtemisClient implements IAuthenticationArtemisClient {
	protected final String username;
	protected final String password;
	protected String token;

	protected final OkHttpClient client;
	protected User assessor;

	public LoginManager(String hostname, String username, String password) {
		super(hostname);
		this.username = username;
		this.password = password;
		// Create without token ..
		this.client = this.createClient(null);
	}

	@Override
	public String getArtemisUrl() {
		return this.getRootURL();
	}

	@Override
	public void login() throws ArtemisClientException {
		if (this.hostname.isBlank() || this.username.isBlank() || this.password.isBlank()) {
			throw new ArtemisClientException("Login without hostname, username, or password is impossible");
		}

		this.token = this.loginViaUsernameAndPassword();
		this.assessor = this.fetchAssessor();
	}

	@Override
	public boolean isLoggedIn() {
		return this.token != null;
	}

	@Override
	public String getToken() {
		return this.token;
	}

	@Override
	public User getUser() {
		return this.assessor;
	}

	protected final User fetchAssessor() throws ArtemisClientException {
		if (this.token == null) {
			return null;
		}
		OkHttpClient clientWithToken = this.createClient(this.token);
		Request request = new Request.Builder().url(this.path("public", "account")).get().build();
		return this.call(clientWithToken, request, User.class);
	}

	protected final String loginViaUsernameAndPassword() throws ArtemisClientException {
		String payload = this.payload(this.getAuthenticationEntity());

		Request request = new Request.Builder() //
				.url(this.path("public", "authenticate")).post(RequestBody.create(payload, JSON)).build();

		try (Response response = this.client.newCall(request).execute()) {
			this.throwIfStatusUnsuccessful(response);
			// jwt=JWT_CONTENT_HERE; Path=/; Max-Age=2592000; Expires=Sun, 26 Feb 2023
			// 23:56:30 GMT; Secure; HttpOnly; SameSite=Lax
			var cookieHeader = response.headers().get("set-cookie");
			if (cookieHeader != null && cookieHeader.startsWith(COOKIE_NAME_JWT)) {
				return cookieHeader.split(";", 2)[0].trim().substring((COOKIE_NAME_JWT + "=").length());
			}
			throw new ArtemisClientException("Authentication was not successful. Cookie not received!");
		} catch (IOException e) {
			throw new ArtemisClientException(e.getMessage(), e);
		}
	}

	private AuthenticationEntity getAuthenticationEntity() {
		AuthenticationEntity entity = new AuthenticationEntity();
		entity.username = this.username;
		entity.password = this.password;
		return entity;
	}

	private static final class AuthenticationEntity implements Serializable {
		@Serial
		private static final long serialVersionUID = -6291795795865534155L;
		@JsonProperty
		private String username;
		@JsonProperty
		private String password;
		@JsonProperty
		private final boolean rememberMe = true;
	}
}
