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

import static java.lang.String.format;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractOfferRequest;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.EdcException;

import de.fraunhofer.iosb.app.model.configuration.Configuration;

/**
 * Send contract offer, negotiation status watch
 */
public class Negotiator {

    private static final String PROTOCOL_IDS_MULTIPART = "ids-multipart";

    private final ConsumerContractNegotiationManager consumerNegotiationManager;
    private final ClientContractNegotiationListener listener;

    /**
     * Class constructor
     * 
     * @param consumerNegotiationManager Initiating a negotiation as a consumer.
     * @param observable Status updates for waiting data transfer requestors to avoid busy waiting.
     */
    public Negotiator(ConsumerContractNegotiationManager consumerNegotiationManager,
            ContractNegotiationObservable observable) {
        this.consumerNegotiationManager = consumerNegotiationManager;

        listener = new ClientContractNegotiationListener();
        observable.registerListener(listener);
    }

    /**
     * Negotiate a contract agreement using the given contract offer.
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
        var contractOfferRequest = ContractOfferRequest.Builder.newInstance()
                .connectorAddress(providerUrl.toString())
                .connectorId(contractOffer.getProvider().toString())
                .contractOffer(contractOffer)
                .protocol(PROTOCOL_IDS_MULTIPART)
                .build();

        var result = consumerNegotiationManager.initiate(contractOfferRequest);
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
