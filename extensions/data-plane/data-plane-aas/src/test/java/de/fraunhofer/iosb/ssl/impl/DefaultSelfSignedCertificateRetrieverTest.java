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
package de.fraunhofer.iosb.ssl.impl;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.security.cert.Certificate;
import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultSelfSignedCertificateRetrieverTest {
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

    private static final String LOCALHOST_URL = String.format("https://localhost:%d", httpsPort);

    private static final String VALID = "https://google.com";
    private static final String EXPIRED = "https://expired.badssl.com";
    private static final String WRONG_HOST = "https://wrong.host.badssl.com";

    @Test
    void getSelfSignedCertificate() {

        Result<Certificate[]> certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(LOCALHOST_URL);

        if (certResult.succeeded()) {
            assertNotNull(certResult.getContent());
        } else {
            fail();
        }
    }

    @Test
    void isTrusted() {
        var trusted = DefaultSelfSignedCertificateRetriever.isTrusted(VALID);

        if (!trusted) {
            fail();
        }
    }

    @Test
    void getExpiredCertificate() {
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(EXPIRED);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("expired"), certResult.getFailureMessages());
        }
    }

    @Test
    void getWrongHostCertificate() {
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(WRONG_HOST);

        if (certResult.succeeded()) {
            fail();
        }
    }
}