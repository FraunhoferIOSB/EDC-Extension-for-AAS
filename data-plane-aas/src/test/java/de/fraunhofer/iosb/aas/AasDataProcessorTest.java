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
package de.fraunhofer.iosb.aas;

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import de.fraunhofer.iosb.testutils.CertificateUtils;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.Mockito.mock;

class AasDataProcessorTest {

    private String aasUrl;

    AasDataProcessor testSubject;
    Service aasService;

    @BeforeEach
    void setUp() throws MessageBusException, EndpointException {
        var port = getFreePort();
        aasUrl = "https://localhost:%s".formatted(port);

        aasService = CertificateUtils.getFaaastService(port);
        aasService.start();


        testSubject = new AllAasDataProcessorFactory(new DefaultSelfSignedCertificateRetriever(),
                mock(OkHttpClient.class),
                RetryPolicy.ofDefaults(),
                new ConsoleMonitor())
                .processorFor(aasUrl);
    }

    @Test
    void testSendAddressOnly() throws IOException {
        // TODO
        try (var response = testSubject.send(getAddress())) {
            response.code();
        }
    }

    @Test
    void testSendWithBody() {
        // TODO
    }

    @Test
    void testSendWithPart() {
        // TODO
    }

    @AfterEach
    void tearDown() {
        aasService.stop();
    }

    private AasDataAddress getAddress() {
        return AasDataAddress.Builder.newInstance()
                .baseUrl(aasUrl)
                .method("GET")
                .referenceChain(
                        new DefaultReference.Builder()
                                .keys(List.of(
                                        new DefaultKey.Builder()
                                                .type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                                                .value("xyz")
                                                .build()
                                ))
                                .build()
                )
                .build();
    }
}