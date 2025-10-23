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
package de.fraunhofer.iosb.aas.lib;

import de.fraunhofer.iosb.dataplane.aas.pipeline.AasTransferRequestBody;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource.Part;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_BODY;
import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_METHOD;
import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_OPERATION;
import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_PATH;

/**
 * Communication to an AAS service via an OkHttpClient.
 */
public class AasDataProcessor {

    private static final String APPLICATION_JSON = "application/json";
    private final EdcHttpClient httpClient;

    AasDataProcessor(EdcHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Get data from an AAS service.
     *
     * @param sourceDataAddress The address of the AAS service.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response getFromAas(AasDataAddress sourceDataAddress) throws IOException {
        String accessUrl = sourceDataAddress.getBaseUrl();

        if (accessUrl == null) {
            throw new IllegalArgumentException("No access url found");
        }

        var requestUrlBuilder = HttpUrl.get(accessUrl).newBuilder();

        var requestPath = sourceDataAddress.getPath();

        if (!requestPath.isEmpty()) {
            // Remove leading forward slash
            requestPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            requestUrlBuilder.addPathSegments(requestPath);
        }

        var requestBuilder = new Request.Builder()
                .headers(Headers.of(sourceDataAddress.getAdditionalHeaders()));

        if (sourceDataAddress.hasProperty(PROXY_OPERATION)) {
            // https://faaast-service.readthedocs.io/en/latest/interfaces/endpoint.html#invoking-operations
            requestUrlBuilder.addPathSegments("invoke/$value");
            byte[] bytes = sourceDataAddress.getStringProperty(PROXY_OPERATION).getBytes(StandardCharsets.UTF_8);
            var requestBody = new AasTransferRequestBody(bytes, APPLICATION_JSON);
            requestBuilder.method("POST", requestBody);
        } else {
            String method = sourceDataAddress.getStringProperty(PROXY_METHOD, sourceDataAddress.getMethod());

            String body = sourceDataAddress.getStringProperty(PROXY_BODY);
            if (body != null) {
                requestBuilder.method(method,
                        HttpMethod.permitsRequestBody(method) ?
                                new AasTransferRequestBody(body.getBytes(StandardCharsets.UTF_8), "application/json") :
                                null);
            } else {
                requestBuilder.method(method, null);
            }

            requestUrlBuilder.addPathSegments(sourceDataAddress.getStringProperty(PROXY_PATH, ""));
        }

        var request = requestBuilder
                .url(requestUrlBuilder.build()) // .url(HttpUrl) is marked as "internal"
                // getAdditionalHeaders() includes authentication needed to access the service

                .build();

        return httpClient.execute(request);
    }

    /**
     * Send data to an AAS service.
     *
     * @param destinationDataAddress The address of the AAS service.
     * @param part                   The data to be sent.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(@NotNull AasDataAddress destinationDataAddress, @NotNull Part part) throws IOException {
        var bytes = part.openStream().readAllBytes();
        var mediaType = part.mediaType();

        String accessUrl = destinationDataAddress.getBaseUrl();

        if (accessUrl == null) {
            throw new IllegalArgumentException("No access url found");
        }

        if (!HttpMethod.permitsRequestBody(destinationDataAddress.getMethod())) {
            throw new IllegalArgumentException(String.format("Destination address method does not allow request body: %s",
                    destinationDataAddress.getMethod()));
        }

        var requestUrlBuilder = HttpUrl.get(accessUrl).newBuilder();

        var requestPath = destinationDataAddress.getPath();

        if (!requestPath.isEmpty()) {
            // Remove leading forward slash
            requestPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            requestUrlBuilder.addPathSegments(requestPath);
        }

        var request = new Request.Builder()
                .method(
                        destinationDataAddress.getMethod(),
                        new AasTransferRequestBody(bytes, mediaType))
                .url(requestUrlBuilder.build().url()) // .url(HttpUrl) is marked as "internal"
                // getAdditionalHeaders() includes authentication needed to access the service
                .headers(Headers.of(destinationDataAddress.getAdditionalHeaders()))
                .build();

        return httpClient.execute(request);
    }
}
