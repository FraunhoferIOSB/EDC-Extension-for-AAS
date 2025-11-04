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

import org.eclipse.edc.connector.controlplane.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NegotiatorTest {

    private final ConsumerContractNegotiationManager ccnmMock = mock(ConsumerContractNegotiationManager.class);
    private final ContractNegotiationStore cnsMock = mock(ContractNegotiationStore.class);
    private final ContractNegotiationObservable contractNegotiationObservable = new ContractNegotiationObservableImpl();

    private final String assetId = "test-asset-id";
    private final Policy mockPolicy = buildPolicy();
    private final ContractNegotiation negotiation = getContractNegotiation();

    private Negotiator clientNegotiator;

    @BeforeEach
    void initializeClientNegotiator() {
        defineMockBehaviour();
        clientNegotiator = new Negotiator(ccnmMock, cnsMock);
    }

    void defineMockBehaviour() {
        when(cnsMock.queryAgreements(any())).thenReturn(Stream.of());
        when(ccnmMock.initiate(any()))
                .thenReturn(StatusResult.success(negotiation));
    }

    @Test
    void testNegotiate() throws ExecutionException, InterruptedException, URISyntaxException {
        // Mocked EDC negotiation manager returns a future which completes to a
        // successful negotiation (agreement)
        // Input is providerUrl (unimportant), contractOffer. The resulting
        // contractAgreement should have the same
        // policy as our contractOffer (not the same object reference) and the same
        // asset ID
        var fakeUri = new URI("https://example.com/fakeurl");
        var contractOffer = ContractOffer.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .assetId(assetId)
                .policy(mockPolicy)
                .build();

        var contractRequest = ContractRequest.Builder.newInstance()
                .contractOffer(contractOffer)
                .counterPartyAddress(fakeUri.toString())
                .protocol("dataspace-protocol-http")
                .build();

        var future = Executors.newSingleThreadExecutor()
                .submit(() -> clientNegotiator.negotiate(contractRequest));

        // Let the negotiator add a listener to this negotiation.
        // If we don't, the "confirmed" signal will be sent too soon, and the negotiator
        // will never see it
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            fail();
        }
        contractNegotiationObservable.invokeForEach(listener -> listener.finalized(negotiation));

        assertNotNull(future);
        var contractNegotiation = future.get();
        assertNotNull(contractNegotiation);
        assertEquals(mockPolicy, contractNegotiation.getContent().getContractAgreement().getPolicy());
        assertEquals(assetId, contractNegotiation.getContent().getContractAgreement().getAssetId());
    }

    /*
     * Policy containing MOCK as permitted action
     */
    private Policy buildPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("MOCK")
                                .build())
                        .build())
                .build();
    }

    private ContractNegotiation getContractNegotiation() {
        return ContractNegotiation.Builder.newInstance()
                .counterPartyId("mock-counter-party-id")
                .counterPartyAddress("mock-counter-party-address")
                .protocol("mock-protocol")
                .id("mocked-negotiation-id")
                .contractAgreement(ContractAgreement.Builder.newInstance()
                        .id(UUID.randomUUID().toString())
                        .providerId("provider")
                        .consumerId("consumer")
                        .assetId(assetId)
                        .policy(mockPolicy)
                        .build())
                .build();
    }

}
