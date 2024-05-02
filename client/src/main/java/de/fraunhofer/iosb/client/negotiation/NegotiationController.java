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
package de.fraunhofer.iosb.client.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;


/**
 * Provides API for contract negotiation by
 * {@link de.fraunhofer.iosb.client.negotiation.Negotiator the Negotiator
 * class}.
 * For documentation see {@link de.fraunhofer.iosb.client.ClientEndpoint}
 */
public class NegotiationController {

    // How long the client waits for a negotiation to finish (seconds)
    private static final int WAIT_FOR_AGREEMENT_TIMEOUT_DEFAULT = 10;

    private final Config config;

    private final Negotiator negotiator;
    private final ClientContractNegotiationListener listener;

    public NegotiationController(ConsumerContractNegotiationManager consumerNegotiationManager,
                                 ContractNegotiationObservable observable, ContractNegotiationStore contractNegotiationStore,
                                 Config config) {
        this.config = config;
        this.negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationStore);
        this.listener = new ClientContractNegotiationListener();
        observable.registerListener(listener);
    }

    public ContractAgreement negotiateContract(ContractRequest contractRequest)
            throws InterruptedException, ExecutionException {

        var negotiationStatusResult = negotiator.negotiate(contractRequest);
        if (!negotiationStatusResult.succeeded()) {
            throw new EdcException(negotiationStatusResult.getFailureDetail());
        }
        var negotiation = negotiationStatusResult.getContent();
        if (Objects.nonNull(negotiation.getContractAgreement())) {
            return negotiationStatusResult.getContent().getContractAgreement();
        } else {
            return waitForAgreement(negotiation.getId());
        }
    }


    private ContractAgreement waitForAgreement(String negotiationId) throws InterruptedException, ExecutionException {
        var agreementFuture = new CompletableFuture<ContractNegotiation>();
        var timeout = config.getInteger("waitForAgreementTimeout",
                WAIT_FOR_AGREEMENT_TIMEOUT_DEFAULT);

        listener.addListener(negotiationId, agreementFuture);

        try {
            var negotiation = agreementFuture.get(timeout, TimeUnit.SECONDS);
            listener.removeListener(negotiationId);

            return negotiation.getContractAgreement();
        } catch (TimeoutException agreementTimeout) {
            throw new EdcException(
                    format("[Client] Agreement negotiation timed out for negotiation id: %s", negotiationId),
                    agreementTimeout);
        }
    }
}
