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
package de.fraunhofer.iosb.aas.lib.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AllAasDataProcessorFactoryTest {
    static int httpsPort = getFreePort();
    @RegisterExtension
    static WireMockExtension wireMockExtension =
            WireMockExtension.extensionOptions()
                    .options(wireMockConfig()
                            .httpsPort(httpsPort)
                            .httpDisabled(true)
                            .keystorePath("src/test/resources/server.jks")
                            .keystorePassword("changeit")
                            .keyManagerPassword("changeit"))
                    .build();

    @Test
    void testGetFromAasForeign() throws IOException {
        //WireMock.configureFor("localhost", httpsPort);
        var testSubject = new AllAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                mock(OkHttpClient.class), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        URL baseUrl = new URL(String.format("https://localhost:%d", httpsPort));

        // If this fails, certificate could not be retrieved from foreignService
        var processor = testSubject.processorFor(baseUrl.toString());

        try (var response = processor.getContent().getFromAas(getDataAddress(baseUrl))) {
            // This means the HTTP request went through --> no certificate problems etc.
            assertEquals(NOT_FOUND.getStatusCode(), response.code());
        }
    }

    private AasDataAddress getDataAddress(URL baseUrl) {
        return AasDataAddress.Builder.newInstance()
                .aasProvider(new Service.Builder().withUrl(baseUrl).build())
                .method("GET")
                .referenceChain(new DefaultReference())
                .build();
    }
}