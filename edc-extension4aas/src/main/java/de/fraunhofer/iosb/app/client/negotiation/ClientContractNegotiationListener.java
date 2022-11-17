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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationListener;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;

/**
 * Accepts a completableFuture which gets completed upon a result of a contract
 * negotiation with the given negotiationId.
 * Accepted final states of a negotiation are: "Confirmed", "Declined",
 * "Failed".
 * "Approved" is not accepted as a final state.
 */
public class ClientContractNegotiationListener implements ContractNegotiationListener {

    private final Map<String, CompletableFuture<ContractNegotiation>> subscribers;

    /**
     * Make this listener listen to contract negotiation events containing the given
     * negotiationId in the Map, completing the given future on the predefined
     * events
     * "Confirmed", "Declined", "Failed".
     * 
     * @param negotiationId Non-null valid Negotiation ID (will not be checked)
     * @param future        Uncompleted Future
     */
    public ClientContractNegotiationListener(Map<String, CompletableFuture<ContractNegotiation>> subscribers) {
        this.subscribers = subscribers;
    }

    /**
     * Called after a {@link ContractNegotiation} was confirmed.
     *
     * @param negotiation the contract negotiation that has been confirmed.
     */
    public void confirmed(ContractNegotiation negotiation) {
        var negotiationId = negotiation.getId();

        if (subscribers.keySet().contains(negotiationId)) {
            subscribers.get(negotiationId).complete(negotiation);
        }

    }

    /**
     * Called after a {@link ContractNegotiation} was declined.
     *
     * @param negotiation the contract negotiation that has been declined.
     */
    public void declined(ContractNegotiation negotiation) {

        var negotiationId = negotiation.getId();

        if (subscribers.keySet().contains(negotiationId)) {
            subscribers.get(negotiationId).completeExceptionally(
                    new Throwable(format("Negotiation with ID %s was declined.", negotiationId)));
        }
    }

    /**
     * Called after a {@link ContractNegotiation} failed.
     *
     * @param negotiation the contract negotiation that failed.
     */
    public void failed(ContractNegotiation negotiation) {
        var negotiationId = negotiation.getId();

        if (subscribers.keySet().contains(negotiationId)) {
            subscribers.get(negotiationId).completeExceptionally(
                    new Throwable(format("Negotiation with ID %s failed.", negotiationId)));
        }

    }

}
