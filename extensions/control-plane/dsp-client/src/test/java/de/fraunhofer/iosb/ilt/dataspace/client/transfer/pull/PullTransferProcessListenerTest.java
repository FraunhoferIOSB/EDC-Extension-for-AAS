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

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


class PullTransferProcessListenerTest {
    private static final String TRANSFER_PROCESS_ID = "transfer-process-id";
    private static final String UNKNOWN_TRANSFER_PROCESS_ID = "unknown-transfer-process-id";
    private static final String STATE = "TERMINATED";
    private static final String ERROR_DETAIL = "transfer failed";

    private Monitor monitor;
    private PullTransferProcessListener testSubject;
    private TransferProcess transferProcess;
    private TransferProcess unknownTransferProcess;
    private DataAddress dataAddress;


    @BeforeEach
    void setUp() {
        monitor = mock(Monitor.class);
        testSubject = new PullTransferProcessListener(monitor);

        dataAddress = mock(DataAddress.class);
        transferProcess = mock(TransferProcess.class);
        when(transferProcess.getId()).thenReturn(TRANSFER_PROCESS_ID);
        when(transferProcess.getContentDataAddress()).thenReturn(dataAddress);
        when(transferProcess.stateAsString()).thenReturn(STATE);
        when(transferProcess.getErrorDetail()).thenReturn(ERROR_DETAIL);

        unknownTransferProcess = mock(TransferProcess.class);
        when(unknownTransferProcess.getId()).thenReturn(UNKNOWN_TRANSFER_PROCESS_ID);
    }


    @Test
    void registerReturnsPendingFutureThatStartedCompletesTest() {
        var future = testSubject.register(transferProcess);

        assertFalse(future.isDone());

        testSubject.started(transferProcess, null);

        assertTrue(future.isDone());
        assertEquals(dataAddress, future.join());
    }


    @Test
    void registerReplacesPreviouslyRegisteredFutureForSameIdTest() {
        var firstFuture = testSubject.register(transferProcess);
        var secondFuture = testSubject.register(transferProcess);

        assertNotSame(firstFuture, secondFuture);

        testSubject.started(transferProcess, null);

        assertFalse(firstFuture.isDone());
        assertEquals(dataAddress, secondFuture.join());
    }


    @Test
    void startedCompletesRegisteredFutureWithDataAddressTest() {
        var future = testSubject.register(transferProcess);

        testSubject.started(transferProcess, null);

        assertTrue(future.isDone());
        assertEquals(dataAddress, future.join());
    }


    @Test
    void startedDoesNotCompleteFutureForUnknownProcessTest() {
        var future = testSubject.register(transferProcess);

        testSubject.started(unknownTransferProcess, null);

        assertFalse(future.isDone());
    }


    @Test
    void completedLogsMessageForRegisteredProcessTest() {
        var future = testSubject.register(transferProcess);

        testSubject.completed(transferProcess);

        var messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(monitor).info(messageCaptor.capture());
        assertTrue(messageCaptor.getValue().contains(TRANSFER_PROCESS_ID));
        assertFalse(future.isDone());
    }


    @Test
    void completedDoesNotLogMessageForUnknownProcessTest() {
        testSubject.register(transferProcess);

        testSubject.completed(unknownTransferProcess);

        verifyNoInteractions(monitor);
    }


    @Test
    void terminatedCompletesRegisteredFutureExceptionallyTest() {
        var future = testSubject.register(transferProcess);

        testSubject.terminated(transferProcess);

        assertFailedWithEdcException(future);
    }


    @Test
    void terminatedDoesNotCompleteFutureForUnknownProcessTest() {
        var future = testSubject.register(transferProcess);

        testSubject.terminated(unknownTransferProcess);

        assertFalse(future.isDone());
    }


    @Test
    void suspendedCompletesRegisteredFutureExceptionallyTest() {
        var future = testSubject.register(transferProcess);

        testSubject.suspended(transferProcess);

        assertFailedWithEdcException(future);
    }


    @Test
    void suspendedDoesNotCompleteFutureForUnknownProcessTest() {
        var future = testSubject.register(transferProcess);

        testSubject.suspended(unknownTransferProcess);

        assertFalse(future.isDone());
    }


    @Test
    void deprovisionedCompletesRegisteredFutureExceptionallyTest() {
        var future = testSubject.register(transferProcess);

        testSubject.deprovisioned(transferProcess);

        assertFailedWithEdcException(future);
    }


    @Test
    void deprovisionedDoesNotCompleteFutureForUnknownProcessTest() {
        var future = testSubject.register(transferProcess);

        testSubject.deprovisioned(unknownTransferProcess);

        assertFalse(future.isDone());
    }


    private void assertFailedWithEdcException(CompletableFuture<DataAddress> future) {
        assertTrue(future.isCompletedExceptionally());
        var exception = assertThrows(CompletionException.class, future::join);
        var cause = exception.getCause();
        assertTrue(cause instanceof EdcException);
        assertTrue(cause.getMessage().contains(TRANSFER_PROCESS_ID));
        assertTrue(cause.getMessage().contains(STATE));
        assertTrue(cause.getMessage().contains(ERROR_DETAIL));
    }
}
