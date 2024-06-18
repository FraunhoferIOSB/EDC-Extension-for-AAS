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
package de.fraunhofer.iosb.app.aas.ssl;

import de.fraunhofer.iosb.app.aas.FaaastServiceManager;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Self-signed certificates should be returned, valid, expired, invalid, ... certificates should not.
 */
class SelfSignedCertificateRetrieverTest {

    private static final String VALID = "https://google.com";
    private static final String EXPIRED = "https://expired.badssl.com";
    private static final String WRONG_HOST = "https://wrong.host.badssl.com";

    @Test
    void getSelfSignedCertificate() throws IOException {
        var faaastMgr = new FaaastServiceManager(new ConsoleMonitor());
        var url = faaastMgr.startService(Path.of("src/test/resources/aasEnvironment.json"), 12345);

        var certResult = SelfSignedCertificateRetriever.getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            assertNotNull(certResult.getContent());
        } else {
            fail();
        }

        faaastMgr.stopServices();
    }

    @Test
    void getValidCertificate() throws MalformedURLException {
        var url = new URL(VALID);
        var certResult = SelfSignedCertificateRetriever.getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("trusted"), certResult.getFailureMessages());
        }
    }

    @Test
    void getExpiredCertificate() throws MalformedURLException {
        var url = new URL(EXPIRED);
        var certResult = SelfSignedCertificateRetriever.getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("expired"), certResult.getFailureMessages());
        }
    }

    @Test
    void getWrongHostCertificate() throws MalformedURLException {
        var url = new URL(WRONG_HOST);
        var certResult = SelfSignedCertificateRetriever.getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        }
    }

}