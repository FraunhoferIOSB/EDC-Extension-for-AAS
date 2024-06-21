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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource.Part;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Communication to an AAS service via an OkHttpClient.
 */
public class AasDataProcessor {

    private final OkHttpClient httpClient;

    AasDataProcessor(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Send data to an AAS service.
     *
     * @param aasDataAddress The address of the AAS service.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(AasDataAddress aasDataAddress) throws IOException {
        return send(aasDataAddress, (byte[]) null, null);
    }

    /**
     * Send data to an AAS service.
     *
     * @param aasDataAddress The address of the AAS service.
     * @param body           The data to be sent.
     * @param mediaType      MediaType of the data to be sent.
     * @return Response by the AAS service.
     * @throws IOException If communication with AAS service fails.
     */
    public Response send(AasDataAddress aasDataAddress, String body, String mediaType) throws IOException {
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
    public Response send(AasDataAddress aasDataAddress, Part part) throws IOException {
        var bytes = part.openStream().readAllBytes();

        return send(aasDataAddress, bytes, part.mediaType());
    }

    private Response send(AasDataAddress aasDataAddress, byte[] bytes, String mediaType) throws IOException {
        var requestBody = new AasTransferRequestBody(bytes, mediaType);

        var request = new Request.Builder().method(
                        aasDataAddress.getMethod(),
                        HttpMethod.permitsRequestBody(aasDataAddress.getMethod()) ? requestBody : null)
                .url(HttpUrl.get(aasDataAddress.getBaseUrl())
                        .newBuilder()
                        .addPathSegment(aasDataAddress.referenceChainAsPath())
                        .build())
                .headers(Headers.of(aasDataAddress.getAdditionalHeaders()))
                .build();

        return httpClient.newCall(request).execute();
    }
}
