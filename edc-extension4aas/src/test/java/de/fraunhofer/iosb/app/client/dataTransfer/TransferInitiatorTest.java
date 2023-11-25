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
package de.fraunhofer.iosb.app.client.dataTransfer;

import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

public class TransferInitiatorTest {

    private final TransferProcessManager mockTransferProcessManager = mock(TransferProcessManager.class);
    private TransferInitiator transferInitiator;
    private StatusResult<TransferProcess> mockStatusResult;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void initializeContractOfferService() throws URISyntaxException {
        URI ownUri = new URI("http://localhost:4321/api/ids");
        transferInitiator = new TransferInitiator(ownUri, mockTransferProcessManager,
                mock(DataTransferObservable.class), mock(CustomAuthenticationRequestFilter.class));
        mockStatusResult = (StatusResult<TransferProcess>) mock(StatusResult.class);
        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
    }

    @Test
    void testInitiateTransferProcess() throws MalformedURLException {
        when(mockStatusResult.failed()).thenReturn(false);
        transferInitiator.initiateTransferProcess(new URL("http://provider-url:1234"), "test-agreement-id",
                "test-asset");
        verify(mockTransferProcessManager, times(1)).initiateConsumerRequest(any());
    }

    @Test
    void testInitiateTransferProcessCustomDataAddress() throws MalformedURLException {
        when(mockStatusResult.failed()).thenReturn(false);
        var dataSink = HttpDataAddress.Builder.newInstance().baseUrl("http://example.com").build();
        transferInitiator.initiateTransferProcess(new URL("http://provider-url:1234"), "test-agreement-id",
                "test-asset", dataSink);
        verify(mockTransferProcessManager, times(1)).initiateConsumerRequest(any());
    }

    @Test
    void testInitiateTransferProcessThrowsEdcExceptionOnFailedTransferInitiation() throws MalformedURLException {
        when(mockStatusResult.failed()).thenReturn(true);
        try {
            transferInitiator.initiateTransferProcess(new URL("http://provider-url:1234"), "test-agreement-id",
                    "test-asset");
            fail();
        } catch (EdcException expected) {
        }
    }
}
