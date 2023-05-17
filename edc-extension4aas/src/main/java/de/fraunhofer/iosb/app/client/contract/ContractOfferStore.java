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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.configuration.Configuration;

/**
 * Contains user added contract offers.
 */
public class ContractOfferStore {
    private final Map<String, ContractOffer> offers;
    private static final Logger LOGGER = Logger.getInstance();

    public ContractOfferStore() {
        this.offers = new ConcurrentHashMap<>();
        loadContractOffers(Configuration.getInstance());
    }

    /**
     * Get all stored offers.
     * 
     * @return Stored offers (non-null but possibly empty)
     */
    public List<ContractOffer> getOffers() {
        return new ArrayList<>(offers.values());
    }

    /**
     * Add offers to the store.
     * 
     * @param newOffers Contract offers to be stored (non-null)
     */
    public void putOffers(ContractOffer... newOffers) {
        Objects.requireNonNull(newOffers, "ContractOffer is null");
        for (ContractOffer newOffer : newOffers) {
            offers.put(newOffer.getId(), newOffer);
        }
    }

    /**
     * Remove an offer
     * 
     * @param contractOfferId Offer ID (non null)
     */
    public void removeOffer(String contractOfferId) {
        Objects.requireNonNull(contractOfferId, "offerId is null");
        offers.remove(contractOfferId);
    }

    /**
     * Update an offer
     * 
     * @param contractOfferId Offer ID (non null)
     * @param contractOffer   The updated contract offer
     */
    public void updateOffer(String contractOfferId, ContractOffer contractOffer) {
        Objects.requireNonNull(contractOfferId, "contractOfferId is null");
        Objects.requireNonNull(contractOffer, "contractOffer is null");
        offers.put(contractOfferId, contractOffer);
    }

    private void loadContractOffers(Configuration config) {
        if (Objects.isNull(config.getAcceptedContractOffersPath())) {
            return;
        }
        var acceptedContractsPath = Path.of(config.getAcceptedContractOffersPath());
        try {
            var mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            var acceptedContractOffers = mapper.readValue(acceptedContractsPath.toFile(),
                    ContractOffer[].class);
            putOffers(acceptedContractOffers);
        } catch (IOException e) {
            LOGGER.warn("[Client] Could not load accepted ContractOffers (edc.ids.client.acceptedContractOfferPaths)",
                    e);
        }
    }
}
