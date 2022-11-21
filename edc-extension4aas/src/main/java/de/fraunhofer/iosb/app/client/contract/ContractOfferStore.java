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
package de.fraunhofer.iosb.app.client.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

/**
 * Contains user added contract offers.
 */
public class ContractOfferStore {
    private final List<ContractOffer> offers;

    public ContractOfferStore() {
        this.offers = new ArrayList<>();
    }

    /**
     * Get all stored offers.
     * 
     * @return Stored offers (not null)
     */
    public List<ContractOffer> getOffers() {
        return offers;
    }

    /**
     * Add an offer to the store.
     * 
     * @param offer A contract offer to be stored (non null)
     */
    public void putOffer(ContractOffer offer) {
        Objects.requireNonNull(offer, "ContractOffer is null");
        offers.add(offer);
    }
}
