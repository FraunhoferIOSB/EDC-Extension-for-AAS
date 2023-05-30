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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NegotiatorTest {

    private final ConsumerContractNegotiationManager consumerNegotiationManager = mock(
            ConsumerContractNegotiationManager.class);
    private final ContractNegotiationStore contractNegotiationStore = mock(
        ContractNegotiationStore.class);
    private final ContractNegotiationObservable observable = new ContractNegotiationObservableImpl();
    private Negotiator clientNegotiator;

    @BeforeEach
    void initializeClientNegotiator() {
        clientNegotiator = new Negotiator(consumerNegotiationManager, observable, contractNegotiationStore);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNegotiate() throws InterruptedException, ExecutionException {
        var negotiationStatusResult = mock(StatusResult.class);
        when(negotiationStatusResult.succeeded()).thenReturn(true);
        var contractNegotiation = mock(ContractNegotiation.class);
        when(contractNegotiation.getId()).thenReturn("test-negotiation-id");
        when(negotiationStatusResult.getContent()).thenReturn(contractNegotiation);

        when(consumerNegotiationManager.initiate(any())).thenReturn(negotiationStatusResult);

        var contractOffer = mock(ContractOffer.class);
        when(contractOffer.getProviderId()).thenReturn("test");

        var agreement = mock(ContractAgreement.class);
        when(contractNegotiation.getContractAgreement()).thenReturn(agreement);
        when(agreement.getId()).thenReturn("agreementId");

        var future = Executors.newSingleThreadExecutor().submit(() -> clientNegotiator.negotiate(new URL("http://testurl:12345"), contractOffer));
        // Let the negotiator think we need time to process
        // If not, the "confirmed" signal will be sent too soon, and the negotiator will
        // never complete
        Thread.sleep(1000);
        observable.invokeForEach(l -> l.accepted(contractNegotiation));

        assertEquals(agreement, future.get());
    }
}
