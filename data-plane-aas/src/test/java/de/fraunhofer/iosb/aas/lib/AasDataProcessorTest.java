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

import de.fraunhofer.iosb.aas.lib.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.dataplane.aas.pipeline.AasPart;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import jakarta.ws.rs.HttpMethod;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class AasDataProcessorTest {

    static AasDataProcessor testSubject;
    static ClientAndServer mockServer;
    static URL aasUrl;

    @BeforeAll
    static void beforeAll() throws MalformedURLException {
        var port = getFreePort();
        mockServer = startClientAndServer(port);
        aasUrl = new URL("https://localhost:%s/api/v3.0".formatted(mockServer.getPort()));

        testSubject = new AllAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                mock(OkHttpClient.class),
                RetryPolicy.ofDefaults(),
                new ConsoleMonitor())
                .processorFor(aasUrl).getContent();

    }

    @AfterAll
    static void tearDown() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() {
        mockServer.reset();
        mockServer.when(request().withMethod("GET")).respond(response().withStatusCode(234));
    }

    @Test
    void testGetFromAasAddressOnly() throws IOException {
        try (var response = testSubject.getFromAas(getAddress())) {
            assertEquals(234, response.code());
        }
    }

    private AasDataAddress getAddress() {
        return AasDataAddress.Builder.newInstance()
                .aasProvider(new Service.Builder().withUrl(aasUrl).build()).method(HttpMethod.GET)
                .referenceChain(new DefaultReference.Builder()
                        .keys(List.of(new DefaultKey.Builder().type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                                .value(UUID.randomUUID().toString()).build()))
                        .build())
                .build();
    }
}