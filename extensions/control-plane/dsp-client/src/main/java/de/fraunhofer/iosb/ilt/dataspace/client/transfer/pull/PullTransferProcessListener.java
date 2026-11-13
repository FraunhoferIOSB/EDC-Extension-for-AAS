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

import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessListener;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


class PullTransferProcessListener implements TransferProcessListener {
    private final Monitor monitor;

    private final Map<String, CompletableFuture<DataAddress>> observedProcesses = new HashMap<>();

    protected PullTransferProcessListener(Monitor monitor) {
        this.monitor = monitor;
    }


    protected CompletableFuture<DataAddress> register(TransferProcess process) {
        CompletableFuture<DataAddress> future = new CompletableFuture<>();
        observedProcesses.put(process.getId(), future);
        return future;
    }


    @Override
    public void started(TransferProcess process, TransferProcessStartedData additionalData) {
        // This means that we have an EDR now.
        if (observedProcesses.containsKey(process.getId())) {
            // Once removed, use data address store
            observedProcesses.remove(process.getId()).complete(process.getContentDataAddress());
        }
    }


    @Override
    public void completed(TransferProcess process) {
        // This should be called once the transfer is done -> for pull, this is too late.
        // Not necessarily a failure state.
        if (observedProcesses.containsKey(process.getId())) {
            monitor.info(String.format("TransferProcess %s completed", process.getId()));
        }
    }


    @Override
    public void terminated(TransferProcess process) {
        completeFailed(process);
    }


    @Override
    public void suspended(TransferProcess process) {
        completeFailed(process);
    }


    @Override
    public void deprovisioned(TransferProcess process) {
        completeFailed(process);
    }


    private void completeFailed(TransferProcess process) {
        if (observedProcesses.containsKey(process.getId())) {
            String failureMessage = String.format("Transfer process %s %s: %s", process.getId(), process.stateAsString(), process.getErrorDetail());
            observedProcesses.remove(process.getId()).completeExceptionally(new EdcException(failureMessage));
        }
    }
}
