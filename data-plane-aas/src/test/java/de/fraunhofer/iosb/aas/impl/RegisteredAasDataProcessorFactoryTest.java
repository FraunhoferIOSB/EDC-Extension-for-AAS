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
package de.fraunhofer.iosb.aas.impl;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;
import de.fraunhofer.iosb.model.aas.service.Service;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import de.fraunhofer.iosb.testutils.TestUtils;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class RegisteredAasDataProcessorFactoryTest {

    @Test
    void testSendRegistered() throws IOException {
        var registeredServices = new HashSet<AasAccessUrl>();
        var testSubject = new RegisteredAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                registeredServices, mock(OkHttpClient.class), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        var port = getFreePort();
        var baseUrl = new URL((System.getProperty("os.name").contains("Windows") ?
                "https://127.0.0.1:%s" : "https://localhost:%s").formatted(port));

        try (var ignored = new TestUtils().startFaaastService(port)) {
            registeredServices.add(new AasAccessUrl(baseUrl));
            var processor = testSubject.processorFor(baseUrl);
            try (var response = processor.getContent().send(getDataAddress(baseUrl))) {
                // processor == null --> Failed getting self-signed cert
                // This means the HTTP request went through --> no certificate problems etc.
                assertNotEquals(500, response.code());
            }
        } catch (MessageBusException | EndpointException | ConfigurationException | AssetConnectionException |
                 PersistenceException e) {
            fail("Failed starting FA³ST service");
        }
    }

    @Test
    void testSendForeign() throws IOException {
        // We trust no service's self-signed certificate
        var registeredServices = new HashSet<AasAccessUrl>();
        var testSubject = new RegisteredAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                registeredServices, new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        var port = getFreePort();
        var baseUrl = new URL("https://127.0.0.1:%s".formatted(port));

        try (var ignored = new TestUtils().startFaaastService(port)) {
            // If this fails, certificate could not be retrieved from foreignService
            var processor = testSubject.processorFor(baseUrl);

            processor.getContent().send(getDataAddress(baseUrl));

            fail("Certificate error expected since EdcHttpClient is used here which does not accept self-signed " +
                    "certificates.");
        } catch (SSLException sslException) {
            // self-signed
        } catch (MessageBusException | EndpointException | ConfigurationException | AssetConnectionException |
                 PersistenceException e) {
            fail("Failed starting FA³ST service");
        }
    }

    private AasDataAddress getDataAddress(URL baseUrl) {
        return AasDataAddress.Builder.newInstance()
                .aasProvider(new Service(baseUrl))
                .method("GET")
                .referenceChain(new DefaultReference())
                .build();
    }
}