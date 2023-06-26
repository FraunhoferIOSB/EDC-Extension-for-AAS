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
package de.fraunhofer.iosb.app.client.negotiation;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequestData;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Send contract offer, negotiation status watch
 */
public class Negotiator {

    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final ClientContractNegotiationListener listener;
    private final ContractNegotiationStore contractNegotiationStore;

    /**
     * Class constructor
     *
     * @param consumerNegotiationManager Initiating a negotiation as a consumer.
     * @param observable                 Status updates for waiting data transfer
     *                                   requesters to avoid busy waiting.
     * @param contractNegotiationStore   Check for existing agreements before negotiating
     */
    public Negotiator(ConsumerContractNegotiationManager consumerNegotiationManager,
                      ContractNegotiationObservable observable, ContractNegotiationStore contractNegotiationStore) {
        this.consumerNegotiationManager = consumerNegotiationManager;
        this.contractNegotiationStore = contractNegotiationStore;

        listener = new ClientContractNegotiationListener();
        observable.registerListener(listener);
    }

    /**
     * Negotiate a contract agreement using the given contract offer if no agreement exists for this constellation.
     *
     * @param providerUrl   The provider of the data.
     * @param contractOffer The object of negotiation.
     * @return contractAgreement of the completed negotiation.
     * @throws ExecutionException   Attempted to retrieve the agreementId but the
     *                              task aborted by throwing an exception. This
     *                              exception can be inspected using the getCause()
     *                              method.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was interrupted.
     */
    public ContractAgreement negotiate(URL providerUrl, ContractOffer contractOffer)
            throws InterruptedException, ExecutionException {
        var contractRequestData = ContractRequestData.Builder.newInstance()
                .connectorId("anonymous")
                .counterPartyAddress(providerUrl.toString())
                .contractOffer(contractOffer)
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .build();

        var contractRequest = ContractRequest.Builder.newInstance()
                .requestData(contractRequestData)
                .build();

        var previousAgreements = contractNegotiationStore.queryAgreements(QuerySpec.max());
        var relevantAgreements = previousAgreements
                .filter(agreement -> agreement.getAssetId().equals(contractOffer.getAssetId()))
                .filter(agreement -> agreement.getProviderId().equals(contractOffer.getProviderId()))
                .collect(toList());

        if (relevantAgreements.size() > 0) { // An agreement exists for this asset & provider
            return relevantAgreements.get(0); // Pick first agreement, hope contractNegotiationStore removes invalid
            // agreements
        }

        var result = consumerNegotiationManager.initiate(contractRequest);
        if (result.succeeded()) {
            return waitForAgreement(result.getContent().getId());
        } else {
            throw new EdcException(result.getFailureDetail());
        }
    }

    private ContractAgreement waitForAgreement(String negotiationId) throws InterruptedException, ExecutionException {
        var agreementFuture = new CompletableFuture<ContractNegotiation>();
        listener.addListener(negotiationId, agreementFuture);

        try {
            var negotiation = agreementFuture.get(Configuration.getInstance().getWaitForAgreementTimeout(),
                    TimeUnit.SECONDS);
            listener.removeListener(negotiationId);

            return negotiation.getContractAgreement();
        } catch (TimeoutException agreementTimeoutExceededException) {
            throw new EdcException(format("Waiting for an agreement failed for negotiationId: %s", negotiationId),
                    agreementTimeoutExceededException);
        }
    }
}
