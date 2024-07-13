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
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import de.fraunhofer.iosb.testutils.SelfSignedCertificateProvider;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import javax.net.ssl.SSLException;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class RegisteredAasDataProcessorFactoryTest {


    @Test
    void testSendOwn() throws MessageBusException, EndpointException, IOException {
        var registeredServices = new HashSet<String>();
        var testSubject = new RegisteredAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(), registeredServices, new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        var port = 54239;
        var foreignService = SelfSignedCertificateProvider.getService(port);
        var baseUrl = "https://localhost:%s".formatted(port);
        foreignService.start();
        registeredServices.add(baseUrl);
        // If this fails, certificate could not be retrieved from foreignService
        var processor = testSubject.processorFor(baseUrl);

        try (var response = processor.send(AasDataAddress.Builder.newInstance()
                .baseUrl(baseUrl)
                .method("GET")
                .referenceChain(new DefaultReference())
                .build())) {
            // This means the HTTP request went through --> no certificate problems etc.
            assertNotEquals(500, response.code());
            foreignService.stop();
        }
    }

    @Test
    void testSendForeign() throws MessageBusException, EndpointException, IOException {
        // We trust no service's self-signed certificate
        var registeredServices = new HashSet<String>();
        var testSubject = new RegisteredAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(), registeredServices, new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        var port = 54239;
        var foreignService = SelfSignedCertificateProvider.getService(port);
        var baseUrl = "https://localhost:%s".formatted(port);
        foreignService.start();

        // If this fails, certificate could not be retrieved from foreignService
        var processor = testSubject.processorFor(baseUrl);

        try (var response = processor.send(AasDataAddress.Builder.newInstance()
                .baseUrl(baseUrl)
                .method("GET")
                .referenceChain(new DefaultReference())
                .build())) {
            // Here, a certificate error should pop up since we would use EdcHttpClient here which does not accept foreignService.
            assertNotEquals(500, response.code());
            foreignService.stop();
            fail();
        } catch (SSLException sslException) {
            // self-signed
        }
    }
}