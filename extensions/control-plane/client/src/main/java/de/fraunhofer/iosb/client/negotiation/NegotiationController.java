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
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Provides API for contract negotiation by {@link de.fraunhofer.iosb.client.negotiation.Negotiator the Negotiator class}. For documentation see
 * {@link de.fraunhofer.iosb.client.ClientEndpoint}
 */
public class NegotiationController {

    // How long the client waits for a negotiation to finish (seconds)
    private static final int WAIT_FOR_AGREEMENT_TIMEOUT_DEFAULT = 20;

    private final Config config;

    private final Negotiator negotiator;
    private final ClientContractNegotiationListener listener;


    public NegotiationController(ConsumerContractNegotiationManager consumerNegotiationManager,
                                 ContractNegotiationObservable observable,
                                 ContractNegotiationStore contractNegotiationStore,
                                 Config config) {
        this.config = config;
        this.negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationStore);
        this.listener = new ClientContractNegotiationListener();
        observable.registerListener(listener);
    }


    public Result<ContractAgreement> negotiateContract(ContractRequest contractRequest) {
        var negotiationStatusResult = negotiator.negotiate(contractRequest);
        if (!negotiationStatusResult.succeeded()) {
            return Result.failure(negotiationStatusResult.getFailureDetail());
        }

        var negotiation = negotiationStatusResult.getContent();
        if (Objects.nonNull(negotiation.getContractAgreement())) {
            return Result.success(negotiationStatusResult.getContent().getContractAgreement());
        }
        else {
            return waitForAgreement(negotiation.getId());
        }
    }


    private Result<ContractAgreement> waitForAgreement(String negotiationId) {
        var agreementFuture = new CompletableFuture<ContractNegotiation>();
        var timeout = config.getInteger("waitForAgreementTimeout", WAIT_FOR_AGREEMENT_TIMEOUT_DEFAULT);

        listener.addListener(negotiationId, agreementFuture);
        ContractNegotiation negotiation;
        try {
            negotiation = agreementFuture.get(timeout, TimeUnit.SECONDS);
        }
        catch (TimeoutException timeoutWhileWaitingException) {
            return Result.failure("Timed out while waiting for agreement: %s".formatted(timeoutWhileWaitingException.getMessage()));
        }
        catch (InterruptedException interruptedWhileWaitingException) {
            return Result.failure("Interrupted while waiting for agreement: %s".formatted(interruptedWhileWaitingException.getMessage()));
        }
        catch (ExecutionException executionException) {
            return Result.failure("ExecutionException thrown while waiting for agreement: %s".formatted(executionException.getMessage()));
        }

        listener.removeListener(negotiationId);
        return Result.success(negotiation.getContractAgreement());
    }
}
