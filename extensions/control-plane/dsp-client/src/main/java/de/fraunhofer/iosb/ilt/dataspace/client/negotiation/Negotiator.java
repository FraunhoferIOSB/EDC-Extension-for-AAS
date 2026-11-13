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
package de.fraunhofer.iosb.ilt.dataspace.client.negotiation;

import de.fraunhofer.iosb.ilt.dataspace.client.config.NegotiationConfiguration;
import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Send contract request, negotiation status watch
 */
public class Negotiator {
    private static final int AGREEMENT_TIMEOUT_DEFAULT = 20;

    private final ContractNegotiationService contractNegotiationService;
    private final ContractNegotiationStore contractNegotiationStore;
    private final ParticipantContextStore participantContextStore;
    private final CounterPartyAddressStore counterPartyAddressStore;
    private final DspClientContractNegotiationListener listener;
    private final Integer timeout;

    /**
     * Class constructor
     *
     * @param contractNegotiationService Initiating a negotiation as a consumer.
     * @param contractNegotiationStore Check for existing agreements before negotiating
     * @param participantContextStore the participant context store used for negotiations.
     */
    public Negotiator(ContractNegotiationService contractNegotiationService, ContractNegotiationStore contractNegotiationStore,
                      ParticipantContextStore participantContextStore, CounterPartyAddressStore counterPartyAddressStore,
                      ContractNegotiationObservable observable, NegotiationConfiguration configuration) {
        this.contractNegotiationService = contractNegotiationService;
        this.contractNegotiationStore = contractNegotiationStore;
        this.participantContextStore = participantContextStore;
        this.counterPartyAddressStore = counterPartyAddressStore;
        this.timeout = Objects.requireNonNullElse(configuration.getNegotiationTimeout(), AGREEMENT_TIMEOUT_DEFAULT);
        this.listener = new DspClientContractNegotiationListener();
        observable.registerListener(listener);
    }


    public ServiceResult<ContractAgreement> negotiate(String contextId, ContractRequest contractRequest) {
        var previousAgreements = contractNegotiationStore.queryAgreements(QuerySpec.max());
        var relevantAgreements = previousAgreements
                .filter(agreement -> agreement.getAssetId().equals(contractRequest.getContractOffer().getAssetId()))
                .filter(agreement -> agreement.getProviderId().equals(contractRequest.getProviderId()))
                .toList();

        if (!relevantAgreements.isEmpty()) {
            return ServiceResult.success(relevantAgreements.get(0));
        }

        ServiceResult<ContractNegotiation> negotiation = contractNegotiationService.initiateNegotiation(participantContextStore.findById(contextId)
                .orElseThrow((failure) -> new EdcException(String.format("Could not find participant context for %s: %s", contextId, failure.getReason()))),
                contractRequest);

        if (negotiation.failed()) {
            return ServiceResult.unexpected("Could not initiate negotiation", negotiation.getFailureDetail());
        }

        counterPartyAddressStore.put(contractRequest.getProviderId(), contractRequest.getCounterPartyAddress());
        return waitForAgreement(negotiation.getContent().getId());
    }


    private ServiceResult<ContractAgreement> waitForAgreement(String negotiationId) {
        CompletableFuture<ContractNegotiation> agreementFuture = listener.addListener(negotiationId);
        ContractNegotiation negotiation;
        try {
            negotiation = agreementFuture.get(timeout, TimeUnit.SECONDS);
        }
        catch (TimeoutException timeoutWhileWaitingException) {
            return ServiceResult.unexpected("Timed out while waiting for agreement", timeoutWhileWaitingException.getMessage());
        }
        catch (InterruptedException interruptedWhileWaitingException) {
            return ServiceResult.unexpected("Interrupted while waiting for agreement", interruptedWhileWaitingException.getMessage());
        }
        catch (ExecutionException executionException) {
            return ServiceResult.unexpected("ExecutionException thrown while waiting for agreement", executionException.getMessage());
        }

        listener.removeListener(negotiationId);
        return ServiceResult.success(negotiation.getContractAgreement());
    }
}
