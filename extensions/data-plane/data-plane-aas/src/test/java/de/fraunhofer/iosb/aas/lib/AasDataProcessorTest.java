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

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import de.fraunhofer.iosb.aas.lib.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import jakarta.ws.rs.HttpMethod;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;


class AasDataProcessorTest {

    @RegisterExtension
    protected static WireMockExtension server = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();
    static AasDataProcessor testSubject;
    static URI aasUri;


    @BeforeAll
    static void beforeAll() throws URISyntaxException {
        aasUri = new URI("http://localhost:%s/api/v3.0".formatted(server.getPort()));

        testSubject = new AllAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                new OkHttpClient(),
                RetryPolicy.ofDefaults(),
                new ConsoleMonitor())
                .processorFor(aasUri.toString()).getContent();

    }


    @Test
    void testGetFromAasAddressOnly() throws IOException {
        AasDataAddress address = getAddress();

        String expectedRequestPath = aasUri.getPath() + "/" + address.getPath() + "/";
        server.stubFor(WireMock.get(urlPathEqualTo(expectedRequestPath))
                .willReturn(aResponse()
                        .withStatus(234)));

        try (var response = testSubject.getFromAas(address)) {
            assertEquals(234, response.code());
        }
    }


    private AasDataAddress getAddress() {
        return AasDataAddress.Builder.newInstance()
                .baseUrl(aasUri.toString())
                .method(HttpMethod.GET)
                .reference(new DefaultReference.Builder()
                        .type(ReferenceTypes.MODEL_REFERENCE)
                        .keys(List.of(new DefaultKey.Builder().type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                                .value(UUID.randomUUID().toString()).build()))
                        .build())
                .build();
    }
}
