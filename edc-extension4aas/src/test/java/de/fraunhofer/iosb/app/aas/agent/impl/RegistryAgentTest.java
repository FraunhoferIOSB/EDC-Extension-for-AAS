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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.ilt.faaast.service.model.descriptor.AssetAdministrationShellDescriptor;
import de.fraunhofer.iosb.ilt.faaast.service.model.descriptor.impl.DefaultAssetAdministrationShellDescriptor;
import de.fraunhofer.iosb.ilt.faaast.service.model.descriptor.impl.DefaultEndpoint;
import de.fraunhofer.iosb.ilt.faaast.service.model.descriptor.impl.DefaultProtocolInformation;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;

import static de.fraunhofer.iosb.app.pipeline.PipelineFailure.Type.FATAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

class RegistryAgentTest {
    private static final int PORT = 42042;
    private static final String HTTP_LOCALHOST_8080 = "http://localhost:%s".formatted(PORT);
    private static ClientAndServer mockServer;

    RegistryAgent testSubject;

    @BeforeEach
    void setUp() throws URISyntaxException {

        testSubject = new RegistryAgent(
                new AllAasDataProcessorFactory(
                        new NoOpSelfSignedCertificateRetriever(),
                        new OkHttpClient(),
                        RetryPolicy.ofDefaults(),
                        new ConsoleMonitor()
                ),
                new AasServiceRegistry(
                        new HashSet<>())
        );
    }

    @Test
    void testApplyShellDescriptor() throws MalformedURLException, SerializationException {
        try (var mockServer = startMockRegistryServer()) {
            mockServer.when(
                            request().withMethod("GET").withPath("/api/v3.0/shell-descriptors"))
                    .respond(
                            HttpResponse.response().withBody(
                                    new JsonSerializer().write(
                                            createAasDescriptor())
                            )
                    );

            var result = testSubject.apply(new URL(HTTP_LOCALHOST_8080));

            // TODO test with real shell descriptor.
            var body = result.getContent();
        }
    }

    void testApplySubmodelDescriptor() throws MalformedURLException, SerializationException {
        try (var mockServer = startMockRegistryServer()) {
            mockServer.when(
                            request().withMethod("GET").withPath("/api/v3.0/submodel-descriptors"))
                    .respond(
                            HttpResponse.response().withBody(
                                    new JsonSerializer().write(
                                            createSubmodelDescriptor())
                            )
                    );

            var result = testSubject.apply(new URL(HTTP_LOCALHOST_8080));

            // TODO test with real submodel descriptor.
            var body = result.getContent();
        }
    }


    @Test
    void testApplyNotActuallyRegistry() throws MalformedURLException {
        var result = testSubject.apply(new URL("https://example.com"));

        assertTrue(result.failed());
        assertEquals(FATAL, result.getFailure().getFailureType());
    }


    @Test
    void testApplyUnreachableRegistry() throws MalformedURLException {
        var result = testSubject.apply(new URL("http://anonymous.invalid"));

        assertTrue(result.failed());
        assertEquals(FATAL, result.getFailure().getFailureType());
    }

    private ClientAndServer startMockRegistryServer() {
        return startClientAndServer(PORT);
    }

    private SubmodelDescriptor createSubmodelDescriptor() {
        return null;
    }

    private AssetAdministrationShellDescriptor createAasDescriptor() {
        return new DefaultAssetAdministrationShellDescriptor.Builder()
                .administration(new DefaultAdministrativeInformation.Builder().build())
                .endpoint(new DefaultEndpoint.Builder()
                        ._interface("AAS-3.0")
                        .protocolInformation(
                                new DefaultProtocolInformation.Builder()
                                        .endpointProtocol("HTTPS")
                                        .href("https://localhost:54321")
                                        .build())
                        .build())
                .build();
    }
}