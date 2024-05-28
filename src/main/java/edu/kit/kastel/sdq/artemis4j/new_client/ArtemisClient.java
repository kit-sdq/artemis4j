package edu.kit.kastel.sdq.artemis4j.new_client;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.AuthenticationDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.CourseDTO;
import edu.kit.kastel.sdq.artemis4j.dto.artemis.UserDTO;
import edu.kit.kastel.sdq.artemis4j.new_api.ArtemisInstance;
import edu.kit.kastel.sdq.artemis4j.new_api.Course;
import edu.kit.kastel.sdq.artemis4j.new_api.User;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ArtemisClient {
    public static final ObjectMapper MAPPER = createObjectMapper();

    private static final Logger log = LoggerFactory.getLogger(ArtemisClient.class);
    private static final String COOKIE_NAME_JWT = "jwt";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final ArtemisInstance artemis;
    private final String jwtToken;
    private final OkHttpClient client;

    private final LazyNetworkValue<User> assessor;
    private final LazyNetworkValue<List<Course>> courses;

    public static ArtemisClient fromUsernamePassword(ArtemisInstance artemis, String username, String password) throws ArtemisNetworkException {
        // The client is just used for login
        var client = new OkHttpClient();

        var payload = ArtemisClient.encodeJSON(new AuthenticationDTO(username, password));
        var request = new Request.Builder()
                .url(artemis.url(List.of("public", "authenticate"), null))
                .post(payload).build();

        String jwtToken;
        try (var response = client.newCall(request).execute()) {
            throwIfStatusUnsuccessful(response);

            // jwt=JWT_CONTENT_HERE; Path=/; Max-Age=2592000; Expires=Sun, 26 Feb 2023
            // 23:56:30 GMT; Secure; HttpOnly; SameSite=Lax
            var cookieHeader = response.headers().get("set-cookie");
            if (cookieHeader == null || !cookieHeader.startsWith(COOKIE_NAME_JWT)) {
                throw new ArtemisNetworkException("Authentication was not successful. Cookie not received!");
            }
            jwtToken = cookieHeader.split(";", 2)[0].trim().substring((COOKIE_NAME_JWT + "=").length());
        } catch (IOException ex) {
            throw new ArtemisNetworkException(ex);
        }

        return new ArtemisClient(artemis, jwtToken);
    }

    public ArtemisClient(ArtemisInstance artemis, String jwtToken) throws ArtemisNetworkException {
        this.artemis = artemis;
        this.jwtToken = jwtToken;
        this.client = buildHttpClient(artemis, jwtToken);
        this.assessor = new LazyNetworkValue<>(() -> new User(UserDTO.getAssessingUser(this)));
        this.courses = new LazyNetworkValue<>(() -> CourseDTO.fetchAll(this).stream().map(dto -> new Course(dto, this)).toList());
    }

    public ArtemisInstance getInstance() {
        return this.artemis;
    }

    public List<Course> getCourses() throws ArtemisNetworkException {
        return this.courses.get();
    }

    public User getAssessor() throws ArtemisNetworkException {
        return this.assessor.get();
    }

    public <R> R call(Request request, Class<R> resultClass) throws ArtemisNetworkException {
        log.info("{} request to '{}'", request.method(), request.url());
        try (var response = client.newCall(request).execute()) {
            log.info("Got response code {}", response.code());
            throwIfStatusUnsuccessful(response);
            if (resultClass == null) {
                return null;
            }

            return this.decodeJSON(response.body().string(), resultClass);
        } catch (IOException e) {
            throw new ArtemisNetworkException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <R> R decodeJSON(String json, Class<R> resultClass) throws ArtemisNetworkException {
        if (resultClass == String.class ) {
            return (R) json;
        }

        try {
            return MAPPER.readValue(json, resultClass);
        } catch (JsonProcessingException e) {
            log.error("Failed to decode JSON: '" + json + "'", e);
            throw new ArtemisNetworkException(e);
        }
    }

    public static <E> RequestBody encodeJSON(E entity) throws ArtemisNetworkException {
        if (entity == null) {
            // Artemis allows empty bodies, okhttp doesn't for anything else than GET
            return RequestBody.create("", JSON);
        }

        try {
            return RequestBody.create(MAPPER.writeValueAsString(entity), JSON);
        } catch (JsonProcessingException e) {
            throw new ArtemisNetworkException(e);
        }
    }

    public static void throwIfStatusUnsuccessful(Response response) throws ArtemisNetworkException {
        if (!response.isSuccessful()) {
            try {
                throw new ArtemisNetworkException("Got response code " + response.code() + " with body " + response.body().string());
            } catch (IOException e) {
                log.error("Failed to decode the Artemis error response body", e);
                throw new ArtemisNetworkException("Got response code " + response.code() + " with an unreadable body");
            }
        }
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper oom = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .addModule(new JavaTimeModule())
                .addModule(new ParameterNamesModule())
                .build()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        oom.setVisibility(oom.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.ANY));

        return oom;
    }

    private static OkHttpClient buildHttpClient(ArtemisInstance artemis, String jwtToken) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .callTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS);

        builder.cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                // NOP
            }

            @NotNull
            @Override
            public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                return List.of(new Cookie.Builder()
                        .domain(artemis.getDomain())
                        .path("/")
                        .name(COOKIE_NAME_JWT)
                        .value(jwtToken)
                        .httpOnly()
                        .secure()
                        .build());
            }
        });

        return builder.build();
    }
}