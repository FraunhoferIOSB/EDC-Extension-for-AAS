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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Since in contract agreements only the counterPartyId is stored, we store the counterPartyAddress separately. This
 * store maps counterPartyId to counterPartyAddress, the address
 * being the DSP endpoint of the counterParty's control plane.
 */
public class CounterPartyAddressStore {

    private final ContractNegotiationStore contractNegotiationStore;

    private final Map<String, String> store;

    public CounterPartyAddressStore(ContractNegotiationStore contractNegotiationStore) {
        this.contractNegotiationStore = contractNegotiationStore;
        this.store = new ConcurrentHashMap<>();
    }


    /**
     * Add new mapping.
     *
     * @param id counterParty id
     * @param address counterParty address
     */
    public void put(String id, String address) {
        store.put(id, address);
    }


    /**
     * Get address by agreement id
     *
     * @param agreementId agreement id
     * @return counterParty address or null if no mapping exists.
     */
    public String getByAgreementId(String agreementId) {
        ContractAgreement agreement = contractNegotiationStore.findContractAgreement(agreementId);
        if (agreement == null) {
            throw new EdcException(String.format("Agreement %s not found", agreementId));
        }
        return store.get(agreement.getProviderId());
    }


    /**
     * Get address by counter-party id
     *
     * @param counterPartyId counter-party id
     * @return counterParty address or null if no mapping exists.
     */
    public String getByCounterPartyId(String counterPartyId) {
        return store.get(counterPartyId);
    }
}
