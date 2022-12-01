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
package de.fraunhofer.iosb.app.client.storage;

import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;

import de.fraunhofer.iosb.app.util.Pair;

/**
 * Storage of agreements previously obtained by contract negotiations.
 * Agreements are stored with their respective provider URL and assetID as key.
 */
public class AgreementStore {
    private final Map<Pair<URL, String>, ContractAgreement> agreements;

    public AgreementStore() {
        agreements = new ConcurrentHashMap<>();
    }

    /**
     * Get all stored agreements
     * 
     * @return non null map of agreements
     */
    public Map<Pair<URL, String>, ContractAgreement> getAll() {
        return agreements;
    }

    /**
     * Get all stored agreements by a provider
     * 
     * @param providerUrl Provider whose agreements are to be fetched
     * @return non null map of agreements
     */
    public Map<Pair<URL, String>, ContractAgreement> getAllProviderAgreements(URL providerUrl) {
        return agreements.entrySet().stream().filter(entry -> providerUrl.equals(entry.getKey().getFirst()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Get all stored agreements
     * 
     * @return non null map of agreements
     */
    public Map<Pair<URL, String>, ContractAgreement> getAllAgreements() {
        return agreements;
    }

    /**
     * Add an agreement to the store
     * 
     * @param provider  non null provider url
     * @param assetId   non null assetId
     * @param agreement non null agreement
     */
    public void addAgreement(URL provider, String assetId, ContractAgreement agreement) {
        agreements.put(new Pair<>(provider, assetId), agreement);
    }

    /**
     * Get an agreement from the store
     * 
     * @param provider  non null provider url
     * @param agreement non null assetID
     */
    public ContractAgreement getAgreement(URL provider, String assetId) {
        return agreements.get(new Pair<>(provider, assetId));
    }

    /**
     * Update an agreement in the store
     * 
     * @param provider  non null provider url
     * @param agreement non null updated agreement
     */
    public void updateAgreement(URL provider, ContractAgreement agreement) {
        agreements.put(new Pair<>(provider, agreement.getAssetId()), agreement);
    }

    /**
     * Remove an agreement in the store
     * 
     * @param agreementId non null agreement to be removed
     */
    public boolean removeAgreement(String agreementId) {
        return agreements.entrySet().removeIf(entry -> agreementId.equals(entry.getValue().getId()));
    }
}
