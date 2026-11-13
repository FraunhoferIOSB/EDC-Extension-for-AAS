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
import de.fraunhofer.iosb.ilt.dataspace.client.transfer.TransferInitiator;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PULL;


public class PullTransferInitiator extends TransferInitiator {

    private static final int TRANSFER_TIMEOUT_DEFAULT = 20;

    private final Integer timeout;
    private final PullTransferProcessListener listener;

    /**
     * Creates a pull transfer initiator.
     *
     * @param transferProcessService execute transfer requests.
     * @param participantContextStore execute commands as different participants in one runtime
     * @param counterPartyAddressStore resolve counter-party address from its id
     * @param transferProcessObservable observe transfer process states
     * @param monitor logging
     * @param configuration transfer initiator configuration
     */
    public PullTransferInitiator(TransferProcessService transferProcessService, ParticipantContextStore participantContextStore,
                                 CounterPartyAddressStore counterPartyAddressStore, TransferProcessObservable transferProcessObservable, Monitor monitor,
                                 TransferConfiguration configuration) {
        super(transferProcessService, participantContextStore, counterPartyAddressStore);
        this.timeout = Objects.requireNonNullElse(configuration.getTransferTimeout(), TRANSFER_TIMEOUT_DEFAULT);
        this.listener = new PullTransferProcessListener(monitor);
        transferProcessObservable.registerListener(listener);
    }


    public StatusResult<DataAddress> startTransferProcess(String participantContextId, String agreementId) {
        TransferProcess transferProcess = initiate(participantContextId, buildRequest(agreementId));
        return waitForTransfer(listener.register(transferProcess));
    }


    private StatusResult<DataAddress> waitForTransfer(CompletableFuture<DataAddress> dataAddressFuture) {
        try {
            return StatusResult.success(dataAddressFuture.get(timeout, TimeUnit.SECONDS));
        }
        catch (TimeoutException timeoutException) {
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, timeoutException.getMessage());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return StatusResult.failure(ResponseStatus.ERROR_RETRY, String.format("Interrupted while waiting for transfer process to start: %s", e.getMessage()));
        }
        catch (ExecutionException e) {
            return StatusResult.fatalError(String.format("%s while waiting for transfer to start: %s", e.getCause().getClass().getSimpleName(),
                    e.getMessage()));
        }
    }


    private TransferRequest buildRequest(String agreementId) {
        return TransferRequest.Builder.newInstance()
                .protocol(getTransferProtocol())
                .contractId(agreementId)
                .counterPartyAddress(getCounterPartyAddress(agreementId))
                // TODO make this configurable / via API / ...
                .transferType(String.format("%s-%s", "ProxyHttpData", PULL))
                .build();
    }
}
