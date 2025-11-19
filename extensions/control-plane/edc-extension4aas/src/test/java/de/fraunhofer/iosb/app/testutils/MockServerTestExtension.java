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
package de.fraunhofer.iosb.app.testutils;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.json.JsonApiSerializer;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;


public class MockServerTestExtension {

    protected final Monitor monitor = new ConsoleMonitor().withPrefix("test");
    private final JsonApiSerializer jsonApiSerializer = new JsonApiSerializer();

    @RegisterExtension
    protected static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();


    protected void mockResponse(METHOD method, String requestPath, Object responseBody, int responseStatus) throws SerializationException,
            UnsupportedModifierException {
        switch (method) {
            case GET -> mockGetResponse(requestPath, responseBody, responseStatus);
            case PUT -> mockPutResponse(requestPath, responseBody, responseStatus);
            case DELETE -> mockDeleteResponse(requestPath, responseStatus);
            case POST -> mockPostResponse(requestPath, responseBody, responseStatus);
        }
    }


    private void mockGetResponse(String requestPath, Object responseBody, int responseStatus) throws SerializationException,
            UnsupportedModifierException {
        server.stubFor(WireMock.get(urlPathEqualTo(requestPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonApiSerializer.write(responseBody))
                        .withStatus(responseStatus)));
    }


    private void mockPutResponse(String requestPath, Object responseBody, int responseStatus) throws SerializationException,
            UnsupportedModifierException {
        server.stubFor(WireMock.put(urlPathEqualTo(requestPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonApiSerializer.write(responseBody))
                        .withStatus(responseStatus)));
    }


    private void mockDeleteResponse(String requestPath, int responseStatus) {
        server.stubFor(WireMock.delete(urlPathEqualTo(requestPath))
                .willReturn(aResponse()
                        .withStatus(responseStatus)));
    }


    private void mockPostResponse(String requestPath, Object responseBody, int responseStatus) throws SerializationException,
            UnsupportedModifierException {
        server.stubFor(WireMock.post(urlPathEqualTo(requestPath))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonApiSerializer.write(responseBody))
                        .withStatus(responseStatus)));
    }


    protected static URI getUri() {
        return URI.create(server.baseUrl());
    }


    protected enum METHOD {
        GET,
        PUT,
        POST,
        DELETE
    }
}
