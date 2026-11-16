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
package de.fraunhofer.iosb.ilt.dataspace.client.testutils;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiationStates;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessStartedData;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.junit.extensions.RuntimeExtension;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.time.Instant;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Drives the real dsp-client production flow inside a booted runtime: polls until the production code has reached a
 * certain point (e.g. a fake captured the contract request), seeds the real {@link ContractNegotiationStore} with an
 * agreement so the transfer step can resolve the counter-party address, and fires the real
 * {@code ContractNegotiationObservable}/{@code TransferProcessObservable} events that finish the blocked HTTP call.
 * Firing is repeated until an exit condition holds (mirroring the busy-poll style of NegotiatorTest and
 * PullTransferInitiatorTest) because the production listeners register their futures only after the service call
 * returned.
 */
public final class NegotiationFlow {

    private static final long DEFAULT_DEADLINE_MS = 10000;
    private static final long POLL_INTERVAL_MS = 25;

    private NegotiationFlow() {}


    /**
     * Polls the value supplier (which returns null until the production code has made the expected call) until it
     * has been populated, and returns the captured value.
     */
    public static <T> T awaitValue(Supplier<T> valueSupplier, String description) {
        var deadline = System.currentTimeMillis() + DEFAULT_DEADLINE_MS;
        T captured = valueSupplier.get();
        while (captured == null && System.currentTimeMillis() < deadline) {
            captured = valueSupplier.get();
            if (captured == null) {
                sleepQuietly();
            }
        }
        assertNotNull(captured, description + " was not captured within " + DEFAULT_DEADLINE_MS + " ms");
        return captured;
    }


    /**
     * Repeatedly invokes an observable event until the exit condition holds (the production flow has progressed past
     * the event) and asserts it eventually did.
     */
    public static void fireUntil(BooleanSupplier condition, String description, Runnable fireAction) {
        var deadline = System.currentTimeMillis() + DEFAULT_DEADLINE_MS;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            fireAction.run();
            sleepQuietly();
        }
        assertTrue(condition.getAsBoolean(), description + " did not progress within " + DEFAULT_DEADLINE_MS + " ms after firing events");
    }


    /**
     * Builds an agreement instance as handed over by a provider: the id doubles as transfer contract id, the policy
     * is the negotiated offer policy and the provider id must equal the offer policy assigner so the real
     * {@code CounterPartyAddressStore} resolves the counter-party address for the follow-up transfer.
     */
    public static ContractAgreement agreement(String agreementId, String providerId, String consumerId, String assetId, Policy policy) {
        return ContractAgreement.Builder.newInstance()
                .id(agreementId)
                .providerId(providerId)
                .consumerId(consumerId)
                .contractSigningDate(Instant.now().getEpochSecond())
                .assetId(assetId)
                .policy(policy)
                .build();
    }


    /**
     * Builds the finalized negotiation a provider would publish: same id and identifiers as the initiated one, with
     * the agreement attached and state FINALIZED.
     */
    public static ContractNegotiation finalizedNegotiation(ContractNegotiation initiated, ContractAgreement agreement) {
        return ContractNegotiation.Builder.newInstance()
                .id(initiated.getId())
                .type(initiated.getType())
                .protocol(initiated.getProtocol())
                .counterPartyId(initiated.getCounterPartyId())
                .counterPartyAddress(initiated.getCounterPartyAddress())
                .participantContextId(initiated.getParticipantContextId())
                .state(ContractNegotiationStates.FINALIZED.code())
                .contractAgreement(agreement)
                .build();
    }


    /**
     * Transitions the initiated negotiation into TERMINATED carrying an error detail, as a provider publishes it when
     * declining the request.
     */
    public static ContractNegotiation terminatedNegotiation(ContractNegotiation initiated, String errorDetail) {
        initiated.transitionTerminating(errorDetail);
        initiated.transitionTerminated();
        return initiated;
    }


    /**
     * Transitions the fake process into TERMINATED with an error detail, as the real transfer state machine would
     * publish it for a failed transfer.
     */
    public static TransferProcess terminatedTransferProcess(TransferProcess process, String errorDetail) {
        process.transitionTerminating(errorDetail);
        process.transitionTerminated();
        return process;
    }


    /**
     * An EDR-style data address as it is attached to a started pull transfer.
     */
    public static DataAddress edrDataAddress(String proxyUrl, String authCode) {
        return DataAddress.Builder.newInstance()
                .type("HttpProxy")
                .property("proxyUrl", proxyUrl)
                .property("authCode", authCode)
                .build();
    }


    /**
     * Saves the negotiation (and with it the agreement) in the real in-memory {@link ContractNegotiationStore} so the
     * transfer step finds the agreement by contract id and keeps the internal event machinery consistent.
     */
    public static void saveNegotiation(RuntimeExtension runtime, ContractNegotiation negotiation) {
        runtime.getService(ContractNegotiationStore.class).save(negotiation);
    }


    /**
     * Removes every stored negotiation so a subsequent test cannot run into the "existing agreement" short-circuit
     * of the production Negotiator.
     */
    public static void cleanupNegotiationStore(RuntimeExtension runtime) {
        var store = runtime.getService(ContractNegotiationStore.class);
        store.queryNegotiations(QuerySpec.max()).forEach(negotiation -> store.deleteById(negotiation.getId()));
    }


    /**
     * Publishes a finalized negotiation through the real observable, reaching the production
     * {@code DspClientContractNegotiationListener} registered by the Negotiator.
     */
    public static void fireNegotiationFinalized(RuntimeExtension runtime, ContractNegotiation negotiation) {
        runtime.getService(ContractNegotiationObservable.class).invokeForEach(listener -> listener.finalized(negotiation));
    }


    /**
     * Publishes a terminated negotiation through the real observable.
     */
    public static void fireNegotiationTerminated(RuntimeExtension runtime, ContractNegotiation negotiation) {
        runtime.getService(ContractNegotiationObservable.class).invokeForEach(listener -> listener.terminated(negotiation));
    }


    /**
     * Publishes a started transfer through the real observable; the started data is handed over because the real
     * {@code TransferProcessEventListener} dereferences it unconditionally when publishing the internal event.
     */
    public static void fireTransferStarted(RuntimeExtension runtime, TransferProcess process, TransferProcessStartedData startedData) {
        runtime.getService(TransferProcessObservable.class).invokeForEach(listener -> listener.started(process, startedData));
    }


    /**
     * Publishes a terminated transfer through the real observable.
     */
    public static void fireTransferTerminated(RuntimeExtension runtime, TransferProcess process) {
        runtime.getService(TransferProcessObservable.class).invokeForEach(listener -> listener.terminated(process));
    }


    private static void sleepQuietly() {
        try {
            Thread.sleep(POLL_INTERVAL_MS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while polling the production flow", e);
        }
    }
}
