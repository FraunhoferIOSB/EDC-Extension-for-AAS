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

import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.command.TerminateNegotiationCommand;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceResult;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Stateful in-process fake for {@link ContractNegotiationService}. Captures the {@link ContractRequest} the
 * production flow submits, returns a successful {@link ServiceResult} carrying a newly built
 * {@link ContractNegotiation} (with a unique id, retrievable via {@link #lastNegotiationId()}), and counts calls.
 * It never completes the negotiation itself: the tests drive the real {@code ContractNegotiationObservable} to
 * bring the open negotiation (registered by the production {@code Negotiator}) to the desired terminal state.
 */
public class FakeContractNegotiationService implements ContractNegotiationService {

    private final AtomicReference<ContractRequest> capturedRequest = new AtomicReference<>();
    private final AtomicReference<ContractNegotiation> lastNegotiation = new AtomicReference<>();
    private final AtomicInteger callCount = new AtomicInteger();

    /**
     * Clears all recorded state.
     */
    public void reset() {
        capturedRequest.set(null);
        lastNegotiation.set(null);
        callCount.set(0);
    }


    public ContractRequest capturedRequest() {
        return capturedRequest.get();
    }


    public String lastNegotiationId() {
        return Objects.requireNonNull(lastNegotiation.get(), "no negotiation initiated yet").getId();
    }


    /**
     * Returns the negotiation instance the fake handed out on the last call: the production Negotiator waits for the
     * completion of a negotiation with exactly this id, so the tests fire observable events for it.
     */
    public ContractNegotiation lastNegotiation() {
        return Objects.requireNonNull(lastNegotiation.get(), "no negotiation initiated yet");
    }


    public int callCount() {
        return callCount.get();
    }


    @Override
    public ServiceResult<ContractNegotiation> initiateNegotiation(ParticipantContext participantContext, ContractRequest request) {
        capturedRequest.set(request);
        callCount.incrementAndGet();
        var negotiation = ContractNegotiation.Builder.newInstance()
                .id("negotiation-%s-%s".formatted(callCount.get(), participantContext.getParticipantContextId()))
                .type(ContractNegotiation.Type.CONSUMER)
                .protocol(request.getProtocol())
                .counterPartyId(Objects.requireNonNullElse(request.getProviderId(), "unknown-provider"))
                .counterPartyAddress(request.getCounterPartyAddress())
                .participantContextId(participantContext.getParticipantContextId())
                .build();
        lastNegotiation.set(negotiation);
        return ServiceResult.success(negotiation);
    }


    @Override
    public ContractNegotiation findbyId(String contractNegotiationId) {
        throw new UnsupportedOperationException("findbyId is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<List<ContractNegotiation>> search(QuerySpec query) {
        throw new UnsupportedOperationException("search is not part of the dsp-client negotiate flow");
    }


    @Override
    public String getState(String negotiationId) {
        throw new UnsupportedOperationException("getState is not part of the dsp-client negotiate flow");
    }


    @Override
    public ContractAgreement getForNegotiation(String negotiationId) {
        throw new UnsupportedOperationException("getForNegotiation is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<Void> terminate(TerminateNegotiationCommand command) {
        throw new UnsupportedOperationException("terminate is not part of the dsp-client negotiate flow");
    }


    @Override
    public ServiceResult<Void> delete(String negotiationId) {
        throw new UnsupportedOperationException("delete is not part of the dsp-client negotiate flow");
    }
}
