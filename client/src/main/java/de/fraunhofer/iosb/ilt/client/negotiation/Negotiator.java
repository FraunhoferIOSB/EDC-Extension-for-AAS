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
package de.fraunhofer.iosb.ilt.client.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;

/**
 * Send contract request, negotiation status watch
 */
public class Negotiator {

    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final ContractNegotiationStore contractNegotiationStore;

    /**
     * Class constructor
     *
     * @param consumerNegotiationManager Initiating a negotiation as a consumer.
     * @param contractNegotiationStore   Check for existing agreements before
     *                                   negotiating
     */
    public Negotiator(ConsumerContractNegotiationManager consumerNegotiationManager,
                      ContractNegotiationStore contractNegotiationStore) {
        this.consumerNegotiationManager = consumerNegotiationManager;
        this.contractNegotiationStore = contractNegotiationStore;
    }

    /*
     * InterruptedException: Thread for agreementId was waiting, sleeping, or
     * otherwise occupied, and was interrupted.
     *
     * ExecutionException: Attempted to retrieve the agreementId but the task
     * aborted by throwing an exception. This exception can be inspected using the
     * getCause() method.
     */
    StatusResult<ContractNegotiation> negotiate(ContractRequest contractRequest) {
        var previousAgreements = contractNegotiationStore.queryAgreements(QuerySpec.max());
        var relevantAgreements = previousAgreements
                .filter(agreement -> agreement.getAssetId().equals(contractRequest.getContractOffer().getAssetId()))
                .filter(agreement -> agreement.getProviderId().equals(contractRequest.getProviderId()))
                .toList();

        if (!relevantAgreements.isEmpty()) {
            // assuming contractNegotiationStore removes invalid agreements
            return StatusResult.success(
                    ContractNegotiation.Builder.newInstance()
                            .contractAgreement(relevantAgreements.get(0))
                            .counterPartyAddress(contractRequest.getCounterPartyAddress())
                            .counterPartyId(contractRequest.getProviderId())
                            .protocol(contractRequest.getProtocol())
                            .build());
        }

        return consumerNegotiationManager.initiate(contractRequest);
    }

}
