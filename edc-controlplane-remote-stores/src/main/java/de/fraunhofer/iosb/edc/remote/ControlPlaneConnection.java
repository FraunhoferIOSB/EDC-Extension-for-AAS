/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.edc.remote;

import de.fraunhofer.iosb.edc.remote.auth.AuthenticationHeaderSupplier;
import de.fraunhofer.iosb.edc.remote.auth.impl.ApiKeyAuthenticationHeaderSupplier;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Connection data object providing the URL from which the EDC control plane is available
 */
public class ControlPlaneConnection {

    private String proto;
    private String hostname;
    private int port;
    private String path;
    private final AuthenticationHeaderSupplier authSupplier;

    public ControlPlaneConnection(String proto, String hostname, int port, String path, String apiKey) {
        this.proto = proto;
        this.hostname = hostname;
        this.port = port;
        this.path = path;
        this.authSupplier = new ApiKeyAuthenticationHeaderSupplier(apiKey);
    }

    public ControlPlaneConnection(String proto, String hostname, int port, String path, String oauthUrl, String clientId, String clientSecret) {
        this.proto = proto;
        this.hostname = hostname;
        this.port = port;
        this.path = path;

        this.authSupplier = null;
        // TODO
    }

    public ControlPlaneConnection(String proto, String hostname, int port, String path) {
        this.proto = proto;
        this.hostname = hostname;
        this.port = port;
        this.path = path;
        if (path.startsWith("/")) {
            this.path = path.substring(1);
        }

        this.authSupplier = null;
        // TODO
    }

    public ControlPlaneConnection(String urlString) throws MalformedURLException {
        var url = new URL(urlString);
        this.proto = url.getProtocol();
        this.hostname = url.getHost();
        this.port = url.getPort();
        this.path = url.getPath();
        if (path.startsWith("/")) {
            this.path = path.substring(1);
        }

        this.authSupplier = null;
        // TODO
    }

    /**
     * Builds a Request with control-plane url and auth filled in.
     *
     * @param method HTTP method for this request
     * @return OkHttp3 request ready to be sent
     */
    public Request prepareRequest(HttpMethod method) {
        return prepareRequest(method, "", null);
    }

    /**
     * Builds a Request with control-plane url and auth filled in.
     *
     * @param method HTTP method for this request
     * @param body   Request body to be sent
     * @return OkHttp3 request ready to be sent
     */
    public Request prepareRequest(HttpMethod method, String body) {
        return prepareRequest(method, "", body);
    }

    /**
     * Builds a Request with control-plane url and auth filled in.
     *
     * @param method         HTTP method for this request
     * @param additionalPath Additional path to specify request, e.g., /assets/request
     * @param body           Request body to be sent
     * @return OkHttp3 request ready to be sent
     */
    public Request prepareRequest(HttpMethod method, String additionalPath, String body) {
        var requestBuilder = new Request.Builder();
        // url
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme(proto)
                .host(hostname)
                .port(port)
                .addPathSegments(path)
                .addPathSegments(additionalPath)
                .build();
        requestBuilder.url(httpUrl);

        // auth
        if (authSupplier != null) {
            var authHeader = authSupplier.getHeader();
            requestBuilder.header(authHeader.getKey(), authHeader.getValue());
        }

        requestBuilder.header("accept", "application/json");

        // body
        RequestBody requestBody = null;
        if (body != null) {
            requestBuilder.header("Content-Type", "application/json");
            requestBody = RequestBody.create(body, MediaType.parse(("application/json")));
        }
        requestBuilder.method(method.name(), requestBody);

        return requestBuilder.build();
    }

    @Override
    public String toString() {
        return "%s://%s:%d/%s".formatted(proto, hostname, port, path);
    }
}
