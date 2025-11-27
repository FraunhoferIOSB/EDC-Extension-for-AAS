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

import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.Monitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DataTransferObservableTest {

    private final Monitor mockMonitor = mock(Monitor.class);
    private DataTransferObservable<String> testSubject;


    @BeforeEach
    void setUp() {
        testSubject = createTestInstance();
    }


    @Test
    void test_register_normalBehavior() {
        // Verify non-exceptional behavior
        var agreementId = UUID.randomUUID().toString();

        CompletableFuture<String> future = testSubject.register(agreementId);

        // Check that future is not complete by accident
        assertEquals("absent", future.getNow("absent"));
    }


    @Test
    void test_register_unregister() {
        // Verify non-exceptional behavior
        var agreementId = UUID.randomUUID().toString();

        testSubject.register(agreementId);
        testSubject.unregister(agreementId);
    }


    @Test
    void test_unregister_nonexistentObserver() {
        // Verify non-exceptional behavior
        testSubject.unregister(UUID.randomUUID().toString());
    }


    @Test
    void test_update_normalBehavior() throws ExecutionException, InterruptedException {
        var agreementId = UUID.randomUUID().toString();
        var data = "test data\r\nhello world";
        var observer = testSubject.register(agreementId);
        testSubject.update(agreementId, data);

        assertEquals(data, observer.get());
    }


    @Test
    void test_update_nullData() throws ExecutionException, InterruptedException {
        var agreementId = UUID.randomUUID().toString();
        var observer = testSubject.register(agreementId);
        testSubject.update(agreementId, null);

        assertNull(observer.get());
    }


    @Test
    void test_terminated_nonexistentObserver() {
        var mockTransferProcess = mock(TransferProcess.class);
        when(mockTransferProcess.getErrorDetail()).thenReturn("Test error detail");
        when(mockTransferProcess.getContractId()).thenReturn("test-contract-id");

        testSubject.terminated(mockTransferProcess);

        verify(mockMonitor).severe(argThat((String message) -> message.contains("Test error detail")));
    }


    private DataTransferObservable<String> createTestInstance() {
        return new DataTransferObservable<>(mockMonitor);
    }

}
