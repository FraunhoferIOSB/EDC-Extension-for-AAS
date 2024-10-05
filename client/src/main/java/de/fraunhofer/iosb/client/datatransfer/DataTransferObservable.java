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

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Gets notified about incoming data by a provider connector.
 * Also notified by the transfer process observable regarding termination of transfer processes
 */
class DataTransferObservable implements TransferProcessListener {

    private final Monitor monitor;

    private final Map<String, CompletableFuture<String>> observers;

    DataTransferObservable(Monitor monitor) {
        this.monitor = monitor;
        observers = new ConcurrentHashMap<>();
    }

    /**
     * Register a future that should complete if a data transfer is finished.
     *
     * @param agreementId The agreement ID this future is dependent on.
     * @return Object containing data in case of transfer.
     */
    CompletableFuture<String> register(String agreementId) {
        observers.put(agreementId, new CompletableFuture<>());
        return observers.get(agreementId);
    }

    /**
     * Unregister an observer.
     *
     * @param agreementId Identifier of the observer.
     */
    void unregister(String agreementId) {
        observers.remove(agreementId);
    }

    /**
     * Notifies an observer if the agreementID matches any of the observer's
     * agreementID.
     *
     * @param agreementId The agreementId coming with a provider's data transfer
     * @param data        Any data by a provider connector
     */
    void update(String agreementId, String data) {
        if (!observers.containsKey(agreementId)) {
            monitor.warning(format(
                    "A POST request to the client's data transfer endpoint with an unknown agreementID was caught. " +
                            "AgreementID: %s",
                    agreementId));
            return;
        }
        observers.get(agreementId).complete(data);
    }

    @Override
    public void terminated(TransferProcess transferProcess) {
        observers.get(transferProcess.getContractId())
                .completeExceptionally(
                        new EdcException("Transfer process terminated. Reason: %s"
                                .formatted(transferProcess.getErrorDetail())));
    }

}
