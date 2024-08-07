/* Licensed under EPL-2.0 2024. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import edu.kit.kastel.sdq.artemis4j.ArtemisNetworkException;
import okhttp3.Request;

public class ArtemisRequest {
    private final String method;
    private List<Object> path;
    private boolean managementRequest = false;
    private final Map<String, Object> requestParams = new HashMap<>();
    private Object body;

    private ArtemisRequest(String method) {
        this.method = method;
    }

    public static ArtemisRequest get() {
        return new ArtemisRequest("GET");
    }

    public static ArtemisRequest post() {
        return new ArtemisRequest("POST");
    }

    public static ArtemisRequest put() {
        return new ArtemisRequest("PUT");
    }

    public static ArtemisRequest delete() {
        return new ArtemisRequest("DELETE");
    }

    public ArtemisRequest path(List<Object> path) {
        this.path = path;
        return this;
    }

    /**
     *
     * @param managementRequest whether the request shall be performed against
     *                          Artemis' "normal" API (/api), or against the
     *                          management API (/management)
     * @return
     */
    public ArtemisRequest managementRequest(boolean managementRequest) {
        this.managementRequest = managementRequest;
        return this;
    }

    public <E> ArtemisRequest body(E entity) {
        if (this.method.equals("GET")) {
            throw new IllegalArgumentException("GET requests cannot have a body");
        }

        this.body = entity;
        return this;
    }

    public ArtemisRequest param(String key, Object value) {
        this.requestParams.put(key, value);
        return this;
    }

    public <R> R executeAndDecode(ArtemisClient client, Class<R> resultClass) throws ArtemisNetworkException {
        var request = new Request.Builder();

        if (this.method.equals("GET")) {
            request.method(this.method, null);
        } else {
            request.method(this.method, ArtemisClient.encodeJSON(this.body));
        }

        request.url(client.getInstance().url(this.path, this.requestParams, this.managementRequest));
        return client.call(request.build(), resultClass);
    }

    public <R> Optional<R> executeAndDecodeMaybe(ArtemisClient client, Class<R> resultClass) throws ArtemisNetworkException {
        // Empty response == failure, so first parse as string and only convert if not
        // blank/empty
        String response = this.executeAndDecode(client, String.class);
        if (!response.isBlank()) {
            return Optional.of(client.decodeJSON(response, resultClass));
        } else {
            return Optional.empty();
        }
    }

    public void execute(ArtemisClient client) throws ArtemisNetworkException {
        this.executeAndDecode(client, null);
    }
}
