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

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.testutils.TestUtils;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.List;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultSelfSignedCertificateRetrieverTest {

    private static final String LOCALHOST_URL =
            System.getProperty("os.name").contains("Windows") ?
                    "https://127.0.0.1:" : "https://localhost:";

    private static final String VALID = "https://google.com";
    private static final String EXPIRED = "https://expired.badssl.com";
    private static final String WRONG_HOST = "https://wrong.host.badssl.com";

    @Test
    void getSelfSignedCertificate() throws MalformedURLException {
        var port = getFreePort();
        var url = new URL(LOCALHOST_URL + port);

        Result<Certificate[]> certResult;
        try (var ignored = new TestUtils().startFaaastService(port)) {
            certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);
        } catch (MessageBusException | EndpointException | ConfigurationException | AssetConnectionException |
                 PersistenceException e) {
            fail("Failed starting FA³ST service");
            return;
        }

        if (certResult.succeeded()) {
            assertNotNull(certResult.getContent());
        } else {
            fail();
        }
    }

    @Test
    void isTrusted() throws MalformedURLException {
        var url = new URL(VALID);
        var trusted = DefaultSelfSignedCertificateRetriever.isTrusted(url);

        if (!trusted) {
            fail();
        }
    }

    @Test
    void getExpiredCertificate() throws MalformedURLException {
        var url = new URL(EXPIRED);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("expired"), certResult.getFailureMessages());
        }
    }

    @Test
    void getWrongHostCertificate() throws MalformedURLException {
        var url = new URL(WRONG_HOST);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        }
    }
}