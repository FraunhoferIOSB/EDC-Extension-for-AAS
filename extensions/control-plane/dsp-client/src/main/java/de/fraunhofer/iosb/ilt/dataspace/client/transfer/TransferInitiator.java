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
package de.fraunhofer.iosb.ilt.dataspace.client.transfer;

import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import org.eclipse.edc.connector.controlplane.services.spi.transferprocess.TransferProcessService;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;

import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;


public abstract class TransferInitiator {
    private final ParticipantContextStore participantContextStore;
    private final TransferProcessService transferProcessService;
    private final CounterPartyAddressStore counterPartyAddressStore;

    protected TransferInitiator(TransferProcessService transferProcessService, ParticipantContextStore participantContextStore,
                                CounterPartyAddressStore counterPartyAddressStore) {
        this.participantContextStore = participantContextStore;
        this.transferProcessService = transferProcessService;
        this.counterPartyAddressStore = counterPartyAddressStore;
    }


    public final TransferProcess initiate(String contextId, TransferRequest request) {
        ServiceResult<TransferProcess> serviceResult = transferProcessService.initiateTransfer(contextFor(contextId), request);
        if (serviceResult.failed()) {
            throw new EdcException(String.format("Initiating transfer request failed: %s", serviceResult.reason()));
        }

        return serviceResult.getContent();
    }


    protected String getTransferProtocol() {
        return DATASPACE_PROTOCOL_HTTP_V_2025_1;
    }


    protected String getCounterPartyAddress(String agreementId) {
        return counterPartyAddressStore.getByAgreementId(agreementId);
    }


    protected ParticipantContext contextFor(String participantId) {
        return participantContextStore.findById(participantId).orElseThrow((failure) -> new EdcException(
                String.format("Could not retrieve participant context for id %s. Failure message: %s", participantId, failure.getReason())));
    }

}
