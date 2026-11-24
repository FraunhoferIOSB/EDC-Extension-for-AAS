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
package de.fraunhofer.iosb.ilt.dataspace.client.store;

import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class CounterPartyAddressStoreTest {
    private static final String COUNTER_PARTY_ID = "counter-party-id";
    private static final String UNKNOWN_COUNTER_PARTY_ID = "unknown-counter-party-id";
    private static final String COUNTER_PARTY_ADDRESS = "https://provider.example.com/dsp";
    private static final String UPDATED_COUNTER_PARTY_ADDRESS = "https://updated.example.com/dsp";
    private static final String AGREEMENT_ID = "agreement-id";
    private static final String PROVIDER_ID = "provider-id";

    private ContractNegotiationStore contractNegotiationStore;
    private CounterPartyAddressStore testSubject;


    @BeforeEach
    void setUp() {
        contractNegotiationStore = mock(ContractNegotiationStore.class);
        testSubject = new CounterPartyAddressStore(contractNegotiationStore);
    }


    @Test
    void putMakesAddressRetrievableByCounterPartyIdTest() {
        testSubject.put(COUNTER_PARTY_ID, COUNTER_PARTY_ADDRESS);

        assertEquals(COUNTER_PARTY_ADDRESS, testSubject.getByCounterPartyId(COUNTER_PARTY_ID));
    }


    @Test
    void putReplacesPreviouslyStoredAddressForSameIdTest() {
        testSubject.put(COUNTER_PARTY_ID, COUNTER_PARTY_ADDRESS);
        testSubject.put(COUNTER_PARTY_ID, UPDATED_COUNTER_PARTY_ADDRESS);

        assertEquals(UPDATED_COUNTER_PARTY_ADDRESS, testSubject.getByCounterPartyId(COUNTER_PARTY_ID));
    }


    @Test
    void getByAgreementIdReturnsAddressOfAgreementProviderTest() {
        var agreement = mock(ContractAgreement.class);
        when(agreement.getProviderId()).thenReturn(PROVIDER_ID);
        when(contractNegotiationStore.findContractAgreement(AGREEMENT_ID)).thenReturn(agreement);
        testSubject.put(PROVIDER_ID, COUNTER_PARTY_ADDRESS);

        assertEquals(COUNTER_PARTY_ADDRESS, testSubject.getByAgreementId(AGREEMENT_ID));
    }


    @Test
    void getByAgreementIdThrowsWhenAgreementIsNotFoundTest() {
        when(contractNegotiationStore.findContractAgreement(AGREEMENT_ID)).thenReturn(null);

        var exception = assertThrows(EdcException.class, () -> testSubject.getByAgreementId(AGREEMENT_ID));

        assertTrue(exception.getMessage().contains(String.format("Agreement %s not found", AGREEMENT_ID)));
    }


    @Test
    void getByCounterPartyIdReturnsStoredAddressTest() {
        testSubject.put(COUNTER_PARTY_ID, COUNTER_PARTY_ADDRESS);

        assertEquals(COUNTER_PARTY_ADDRESS, testSubject.getByCounterPartyId(COUNTER_PARTY_ID));
    }


    @Test
    void getByCounterPartyIdReturnsNullForUnknownCounterPartyIdTest() {
        assertNull(testSubject.getByCounterPartyId(UNKNOWN_COUNTER_PARTY_ID));
    }
}
