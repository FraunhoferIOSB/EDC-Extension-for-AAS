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
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.contractnegotiation.ContractNegotiationService;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class NegotiatorTest {
    private static final String CONTEXT_ID = "test-context-id";
    private static final String NEGOTIATION_ID = "negotiation-id";
    private static final String PROVIDER_ID = "provider-id";
    private static final String ASSET_ID = "asset-id";
    private static final String COUNTER_PARTY_ADDRESS = "https://provider.example.com/dsp";

    private ContractNegotiationService contractNegotiationService;
    private ContractNegotiationStore contractNegotiationStore;
    private ParticipantContextStore participantContextStore;
    private DspClientContractNegotiationListener registeredListener;
    private ContractRequest contractRequest;
    private ContractNegotiation negotiation;
    private ContractAgreement agreement;

    private Negotiator testSubject;

    @BeforeEach
    void setUp() {
        contractNegotiationService = mock();
        contractNegotiationStore = mock();
        participantContextStore = mock();
        ContractNegotiationObservable observable = mock();
        NegotiationConfiguration configuration = mock();
        when(configuration.getNegotiationTimeout()).thenReturn(1);

        testSubject = new Negotiator(contractNegotiationService, contractNegotiationStore, participantContextStore, observable, configuration);

        var listenerCaptor = ArgumentCaptor.forClass(DspClientContractNegotiationListener.class);
        verify(observable).registerListener(listenerCaptor.capture());
        registeredListener = listenerCaptor.getValue();

        contractRequest = mock(ContractRequest.class);
        var contractOffer = mock(ContractOffer.class);
        when(contractRequest.getContractOffer()).thenReturn(contractOffer);
        when(contractRequest.getProviderId()).thenReturn(PROVIDER_ID);
        when(contractRequest.getCounterPartyAddress()).thenReturn(COUNTER_PARTY_ADDRESS);
        when(contractOffer.getAssetId()).thenReturn(ASSET_ID);

        when(contractNegotiationStore.queryAgreements(any())).thenReturn(Stream.empty());
        when(participantContextStore.findById(any())).thenReturn(StoreResult.success(mock(ParticipantContext.class)));

        negotiation = mock(ContractNegotiation.class);
        when(negotiation.getId()).thenReturn(NEGOTIATION_ID);
        agreement = mock(ContractAgreement.class);
        when(negotiation.getContractAgreement()).thenReturn(agreement);
        when(contractNegotiationService.initiateNegotiation(any(), any())).thenReturn(ServiceResult.success(negotiation));
    }


    @Test
    void negotiateReturnsAgreementAfterSuccessfulNegotiationTest() {
        var resultFuture = CompletableFuture.supplyAsync(() -> testSubject.negotiate(CONTEXT_ID, contractRequest));

        var deadline = System.currentTimeMillis() + 5000;
        while (!resultFuture.isDone() && System.currentTimeMillis() < deadline) {
            registeredListener.finalized(negotiation);
        }

        var result = resultFuture.join();

        assertTrue(result.succeeded());
        assertEquals(agreement, result.getContent());
        verify(contractNegotiationService).initiateNegotiation(any(), any());
    }


    @Test
    void negotiateFailsWhenInitiationFailsTest() {
        when(contractNegotiationService.initiateNegotiation(any(), any())).thenReturn(ServiceResult.unexpected("initiation failed"));

        var result = testSubject.negotiate(CONTEXT_ID, contractRequest);

        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("Could not initiate negotiation"));
    }


    @Test
    void negotiateReturnsExistingAgreementTest() {
        var unrelatedAgreement = mock(ContractAgreement.class);
        when(unrelatedAgreement.getAssetId()).thenReturn("other-asset-id");
        when(unrelatedAgreement.getProviderId()).thenReturn(PROVIDER_ID);
        var existingAgreement = mock(ContractAgreement.class);
        when(existingAgreement.getAssetId()).thenReturn(ASSET_ID);
        when(existingAgreement.getProviderId()).thenReturn(PROVIDER_ID);
        when(contractNegotiationStore.queryAgreements(any())).thenReturn(Stream.of(unrelatedAgreement, existingAgreement));

        var result = testSubject.negotiate(CONTEXT_ID, contractRequest);

        assertTrue(result.succeeded());
        assertEquals(existingAgreement, result.getContent());
        verify(contractNegotiationService, never()).initiateNegotiation(any(), any());
    }


    @Test
    void negotiateThrowsWhenParticipantContextIsMissingTest() {
        when(participantContextStore.findById(any())).thenReturn(StoreResult.notFound("context not found"));

        var exception = assertThrows(EdcException.class, () -> testSubject.negotiate(CONTEXT_ID, contractRequest));

        assertTrue(exception.getMessage().contains(String.format("Could not find participant context for %s", CONTEXT_ID)));
        verify(contractNegotiationService, never()).initiateNegotiation(any(), any());
    }


    @Test
    void negotiateFailsWhenNegotiationTerminatedTest() {
        var resultFuture = CompletableFuture.supplyAsync(() -> testSubject.negotiate(CONTEXT_ID, contractRequest));

        var deadline = System.currentTimeMillis() + 5000;
        while (!resultFuture.isDone() && System.currentTimeMillis() < deadline) {
            registeredListener.terminated(negotiation);
        }

        var result = resultFuture.join();

        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("ExecutionException thrown while waiting for agreement"));
    }


    @Test
    void listenerIgnoresEventsForUnknownNegotiationsTest() {
        var unknownNegotiation = mock(ContractNegotiation.class);
        when(unknownNegotiation.getId()).thenReturn("unknown-negotiation-id");

        registeredListener.finalized(unknownNegotiation);
        registeredListener.terminated(unknownNegotiation);
    }


    @Test
    void negotiateTimesOutWhileWaitingForAgreementTest() {
        var result = CompletableFuture.supplyAsync(() -> testSubject.negotiate(CONTEXT_ID, contractRequest)).join();

        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("Timed out while waiting for agreement"));
    }


    @Test
    void negotiateFailsWhenInterruptedWhileWaitingForAgreementTest() {
        var holding = new AtomicReference<ServiceResult<ContractAgreement>>();
        var worker = new Thread(() -> holding.set(testSubject.negotiate(CONTEXT_ID, contractRequest)));
        worker.start();
        worker.interrupt();
        try {
            worker.join(5000);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }

        var result = holding.get();
        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains("Interrupted while waiting for agreement"));
    }
}
