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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Connection data object providing the URL from which the EDC control plane is available
 */
public class ControlPlaneConnection {

    public static final String APPLICATION_JSON = "application/json";
    private static final String CONTENT_TYPE = "content-type";
    private final HttpUrl connectionUri;
    private final String resourceName;

    private final UnaryOperator<Request.Builder> authSupplier;

    public ControlPlaneConnection(URI connectionUri, String resourceName, AuthenticationMethod authenticationMethod) {
        this.connectionUri = Objects.requireNonNull(HttpUrl.parse(connectionUri.toString()));
        this.resourceName = resourceName;

        this.authSupplier = request -> request.headers(Headers.of(Map.ofEntries(authenticationMethod.getHeader())));
    }

    /**
     * Builds a request with control-plane uri and auth.
     *
     * @param method HTTP method for this request
     * @param body   Request body to be sent
     * @return OkHttp3 request ready to be sent
     */
    public Request prepareRequest(HttpMethod method, String body) {
        return prepareRequest(method, "", body);
    }

    /**
     * Builds a request with control-plane uri and auth.
     *
     * @param method         HTTP method for this request
     * @param additionalPath Additional path to specify request, e.g., /request
     * @param body           Request body to be sent
     * @return OkHttp3 request ready to be sent
     */
    public Request prepareRequest(HttpMethod method, String additionalPath, String body) {
        var requestBuilder = new Request.Builder();

        requestBuilder.url(connectionUri.newBuilder()
                .addPathSegments(resourceName)
                .addPathSegments(additionalPath)
                .build());

        // auth
        authSupplier.apply(requestBuilder);

        requestBuilder.header("accept", APPLICATION_JSON);

        // body
        RequestBody requestBody = null;
        if (body != null) {
            requestBuilder.header(CONTENT_TYPE, APPLICATION_JSON);
            requestBody = RequestBody.create(body, MediaType.parse(APPLICATION_JSON));
        }
        requestBuilder.method(method.name(), requestBody);

        return requestBuilder.build();
    }
}
