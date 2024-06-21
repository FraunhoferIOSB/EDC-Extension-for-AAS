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
import okhttp3.RequestBody;
import okhttp3.Response;
import org.eclipse.edc.connector.dataplane.spi.pipeline.DataSource;

import java.io.IOException;

public class AasManipulator {

    private final OkHttpClient httpClient;

    AasManipulator(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Response send(AasDataAddress aasDataAddress, DataSource.Part part) throws IOException {
        var requestBody = toBody(part);

        var request = new Request.Builder().method(aasDataAddress.getMethod(), requestBody)
                .url(HttpUrl.get(aasDataAddress.getBaseUrl())
                        .newBuilder()
                        .addPathSegment(aasDataAddress.referenceChainAsPath())
                        .build())
                .headers(Headers.of(aasDataAddress.getAdditionalHeaders()))
                .build();

        return httpClient.newCall(request).execute();
    }

    private RequestBody toBody(DataSource.Part part) {
        return new AasTransferRequestBody(part::openStream, part.mediaType());
    }

}
