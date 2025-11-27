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
package de.fraunhofer.iosb.client.datatransfer;

import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class TransferInitiatorTest {

    private final TransferProcessManager mockTransferProcessManager = mock(TransferProcessManager.class);

    private TransferInitiator transferInitiator;
    private StatusResult<TransferProcess> mockStatusResult;


    @BeforeEach
    @SuppressWarnings("unchecked")
    void initializeContractOfferService() {
        var configMock = ConfigFactory.fromMap(Map.of("web.http.port", "8080", "web.http.path", "/api"));

        transferInitiator = new TransferInitiator(mock(Monitor.class), configMock, () -> "localhost",
                mockTransferProcessManager);

        mockStatusResult = (StatusResult<TransferProcess>) mock(StatusResult.class);

        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
    }


    @Test
    void testInitiateTransferProcess() throws URISyntaxException {
        when(mockStatusResult.failed()).thenReturn(false);

        transferInitiator.initiateTransferProcess(new URI("http://provider-url:1234"), "test-agreement-id",
                UUID.randomUUID().toString());
        verify(mockTransferProcessManager, times(1)).initiateConsumerRequest(any());
    }


    @Test
    void testInitiateTransferProcessCustomDataAddress() throws URISyntaxException {
        when(mockStatusResult.failed()).thenReturn(false);
        var dataSink = HttpDataAddress.Builder.newInstance().baseUrl("https://example.com").build();
        transferInitiator.initiateTransferProcess(new URI("http://provider-url:1234"),
                "test-agreement-id", dataSink);
        verify(mockTransferProcessManager, times(1)).initiateConsumerRequest(any());
    }

}
