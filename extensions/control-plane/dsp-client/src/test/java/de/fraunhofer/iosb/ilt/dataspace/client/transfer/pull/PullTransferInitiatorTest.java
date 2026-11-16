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
package de.fraunhofer.iosb.ilt.dataspace.client.transfer.pull;

import de.fraunhofer.iosb.ilt.dataspace.client.config.TransferConfiguration;
import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PullTransferInitiatorTest {
    private static final String CONTEXT_ID = "test-context-id";
    private static final String AGREEMENT_ID = "agreement-id";
    private static final String TRANSFER_PROCESS_ID = "transfer-process-id";
    private static final String COUNTER_PARTY_ADDRESS = "https://provider.example.com/dsp";

    private TransferProcessService transferProcessService;
    private ParticipantContextStore participantContextStore;
    private PullTransferProcessListener registeredListener;
    private TransferProcess transferProcess;

    private PullTransferInitiator testSubject;


    @BeforeEach
    void setUp() {
        transferProcessService = mock();
        participantContextStore = mock();
        CounterPartyAddressStore counterPartyAddressStore = mock();
        TransferProcessObservable observable = mock();
        Monitor monitor = mock();
        TransferConfiguration configuration = mock();
        when(configuration.getTransferTimeout()).thenReturn(1);

        testSubject = new PullTransferInitiator(transferProcessService, participantContextStore, counterPartyAddressStore, observable, monitor, configuration);

        var listenerCaptor = ArgumentCaptor.forClass(PullTransferProcessListener.class);
        verify(observable).registerListener(listenerCaptor.capture());
        registeredListener = listenerCaptor.getValue();

        transferProcess = mock();
        when(transferProcess.getId()).thenReturn(TRANSFER_PROCESS_ID);

        when(participantContextStore.findById(any())).thenReturn(StoreResult.success(mock()));
        when(counterPartyAddressStore.getByAgreementId(any())).thenReturn(COUNTER_PARTY_ADDRESS);
        when(transferProcessService.initiateTransfer(any(), any())).thenReturn(ServiceResult.success(transferProcess));
    }


    @Test
    void startTransferProcessReturnsDataAddressAfterCompletionTest() {
        var dataAddress = mock(DataAddress.class);
        when(transferProcess.getContentDataAddress()).thenReturn(dataAddress);
        var resultFuture = CompletableFuture.supplyAsync(() -> testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID));

        var deadline = System.currentTimeMillis() + 5000;
        while (!resultFuture.isDone() && System.currentTimeMillis() < deadline) {
            registeredListener.started(transferProcess, null);
        }

        var result = resultFuture.join();

        assertTrue(result.succeeded());
        assertEquals(dataAddress, result.getContent());

        var requestCaptor = ArgumentCaptor.forClass(TransferRequest.class);
        verify(transferProcessService).initiateTransfer(any(), requestCaptor.capture());
        var request = requestCaptor.getValue();
        assertEquals(AGREEMENT_ID, request.getContractId());
        assertEquals(COUNTER_PARTY_ADDRESS, request.getCounterPartyAddress());
        assertEquals("ProxyHttpData-PULL", request.getTransferType());
    }


    @Test
    void startTransferProcessThrowsWhenInitiationFailsTest() {
        when(transferProcessService.initiateTransfer(any(), any())).thenReturn(ServiceResult.unexpected("initiation failed"));

        var exception = assertThrows(EdcException.class, () -> testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID));

        assertTrue(exception.getMessage().contains("Initiating transfer request failed"));
        verify(transferProcessService).initiateTransfer(any(), any());
    }


    @Test
    void startTransferProcessTimesOutWhileWaitingForTransferToStartTest() {
        var result = CompletableFuture.supplyAsync(() -> testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID)).join();

        assertTrue(result.failed());
        assertEquals(ResponseStatus.ERROR_RETRY, result.getFailure().status());
    }


    @Test
    void startTransferProcessFailsWhenInterruptedWhileWaitingForTransferToStartTest() {
        var holding = new AtomicReference<StatusResult<DataAddress>>();
        var worker = new Thread(() -> holding.set(testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID)));
        worker.start();
        worker.interrupt();
        try {
            worker.join(5000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }

        var result = holding.get();
        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("Interrupted while waiting for transfer process to start"));
    }


    @Test
    void startTransferProcessFailsWhenTransferTerminatedTest() {
        when(transferProcess.stateAsString()).thenReturn("TERMINATED");
        when(transferProcess.getErrorDetail()).thenReturn("transfer failed");

        var resultFuture = CompletableFuture.supplyAsync(() -> testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID));

        var deadline = System.currentTimeMillis() + 5000;
        while (!resultFuture.isDone() && System.currentTimeMillis() < deadline) {
            registeredListener.terminated(transferProcess);
        }

        var result = resultFuture.join();

        assertTrue(result.fatalError());
        assertTrue(result.getFailureDetail().contains("ExecutionException while waiting for transfer to start"));
    }


    @Test
    void startTransferProcessThrowsWhenParticipantContextIsMissingTest() {
        when(participantContextStore.findById(any())).thenReturn(StoreResult.notFound("context not found"));

        var exception = assertThrows(EdcException.class, () -> testSubject.startTransferProcess(CONTEXT_ID, AGREEMENT_ID));

        assertTrue(exception.getMessage().contains("Could not retrieve participant context for id"));
        verify(transferProcessService, never()).initiateTransfer(any(), any());
    }
}
