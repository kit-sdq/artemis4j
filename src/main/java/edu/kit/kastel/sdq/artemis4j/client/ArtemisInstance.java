/* Licensed under EPL-2.0 2024-2025. */
package edu.kit.kastel.sdq.artemis4j.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.HttpUrl;
import org.jspecify.annotations.Nullable;

public class ArtemisInstance {
    // e.g. https://
    private final String protocol;

    // e.g. artemis.kit.edu
    private final String domain;

    public ArtemisInstance(String url) {
        this.protocol = extractProtocol(url);
        this.domain = extractDomain(url, protocol);
    }

    /**
     *
     * @return the protocol with which to communicate with Artemis as a URL prefix,
     *         e.g. https://
     */
    public String getProtocol() {
        return this.protocol;
    }

    /**
     *
     * @return the hostname of the Artemis instance (without protocol!), e.g.
     *         artemis.kit.edu
     */
    public String getDomain() {
        return this.domain;
    }

    public String getAPIBaseURL() {
        return this.protocol + this.domain + "/api";
    }

    public String getManagementBaseURL() {
        return this.protocol + this.domain + "/management";
    }

    public HttpUrl url(List<Object> pathComponents, @Nullable Map<String, Object> queryParams) {
        return this.url(pathComponents, queryParams, false);
    }

    public HttpUrl url(
            List<Object> pathComponents, @Nullable Map<String, Object> queryParams, boolean managementRequest) {
        String baseUrl = managementRequest ? this.getManagementBaseURL() : this.getAPIBaseURL();
        String path = pathComponents.stream().map(Object::toString).collect(Collectors.joining("/"));
        var url = HttpUrl.parse(baseUrl + "/" + path);
        assert url != null;
        if (queryParams != null && !queryParams.isEmpty()) {
            var builder = url.newBuilder();
            queryParams.forEach((p, v) -> builder.addQueryParameter(p, v.toString()));
            url = builder.build();
        }
        return url;
    }

    private static String extractProtocol(String hostname) {
        if (!hostname.contains("://")) {
            return "https://";
        }
        return hostname.split("://", 2)[0] + "://";
    }

    private static String extractDomain(String hostname, String protocol) {
        String finalHostname = hostname.trim();
        if (finalHostname.startsWith(protocol)) {
            finalHostname = finalHostname.substring(protocol.length());
        }

        if (finalHostname.contains("/")) {
            finalHostname = finalHostname.split("/", 2)[0];
        }

        return finalHostname;
    }
}
