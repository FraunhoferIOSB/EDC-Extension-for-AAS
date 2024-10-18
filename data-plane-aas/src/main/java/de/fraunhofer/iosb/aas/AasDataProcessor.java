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
package de.fraunhofer.iosb.aas;

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
     * Send data to an AAS service with no requestBody
     *
     * @param aasDataAddress The address of the AAS service.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(@NotNull AasDataAddress aasDataAddress) throws IOException {
        return send(aasDataAddress, (byte[]) null, null);
    }

    /**
     * Send data to an AAS service.
     *
     * @param aasDataAddress The address of the AAS service.
     * @param body           The data to be sent.
     * @param mediaType      MediaType of the data to be sent. (only relevant if body != null)
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(@NotNull AasDataAddress aasDataAddress, String body, String mediaType) throws IOException {
        var bytes = body == null ? null : body.getBytes(StandardCharsets.UTF_8);
        return send(aasDataAddress, bytes, mediaType);
    }

    /**
     * Send data to an AAS service.
     *
     * @param aasDataAddress The address of the AAS service.
     * @param part           The data to be sent.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(@NotNull AasDataAddress aasDataAddress, @NotNull Part part) throws IOException {
        var bytes = part.openStream().readAllBytes();

        return send(aasDataAddress, bytes, part.mediaType());
    }

    private Response send(AasDataAddress aasDataAddress, byte[] bytes, String mediaType) throws IOException {
        var requestUrlBuilder = HttpUrl.get(aasDataAddress.getBaseUrl()).newBuilder();

        var requestPath = aasDataAddress.getPath();

        if (!requestPath.isEmpty()) {
            // Remove leading forward slash
            requestPath = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
            requestUrlBuilder.addPathSegments(requestPath);
        }

        // This is also called on the destination address, so ensure no payload is present
        if (aasDataAddress.isOperation() && (bytes == null || bytes.length == 0)) {
            // https://faaast-service.readthedocs.io/en/latest/interfaces/endpoint.html#invoking-operations
            requestUrlBuilder.addPathSegments("invoke/$value");
            bytes = aasDataAddress.getOperation().getBytes(StandardCharsets.UTF_8);
            mediaType = APPLICATION_JSON;
        }

        var requestUrl = requestUrlBuilder.build().url();
        var requestBody = new AasTransferRequestBody(bytes, mediaType);

        var request = new Request.Builder()
                .method(
                        aasDataAddress.getMethod(),
                        HttpMethod.permitsRequestBody(aasDataAddress.getMethod()) ? requestBody : null)
                .url(requestUrl) // .url(HttpUrl) is marked as "internal"
                .headers(Headers.of(aasDataAddress.getAdditionalHeaders()))
                .build();

        return httpClient.execute(request);
    }
}
