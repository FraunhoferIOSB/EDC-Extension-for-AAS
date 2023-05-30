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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.client.exception.AmbiguousOrNullException;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

/**
 * Finds out contract offer for a given asset id and provider EDC url
 */
public class ContractOfferService {

    // private static final String ASSET_PROPERTY_ID = "asset:prop:id";
    private static final Configuration configuration = Configuration.getInstance();

    private final CatalogService catalogService;
    private final ContractOfferStore contractOfferStore;

    /**
     * Class constructor
     *
     * @param catalogService Fetching the catalog of a provider.
     */
    public ContractOfferService(CatalogService catalogService) {
        this.catalogService = catalogService;
        this.contractOfferStore = new ContractOfferStore();
    }

    /**
     * Returns an offered contractOffer for this asset by this provider. If zero or
     * multiple contractOffers are found, an {@link AmbiguousOrNullException} is
     * thrown
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return A list of contractOffers offered by the provider for the given
     * assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was interrupted.
     */
    public List<ContractOffer> getContractsForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var catalogFuture = catalogService
                .request(providerUrl.toString(),
                        "ids-multipart",
                        QuerySpec.Builder
                                .newInstance()
                                // .filter(List.of(new Criterion(ASSET_PROPERTY_ID, "=", assetId)))
                                .build());
        byte[] catalogSerialized;
        try {
            catalogSerialized = catalogFuture.get(configuration.getWaitForCatalogTimeout(), TimeUnit.SECONDS);
        } catch (ExecutionException futureExecutionException) {
            throw new EdcException(format("Failed fetching a catalog by provider %s.", providerUrl),
                    futureExecutionException);
        } catch (TimeoutException timeoutCatalogFutureGetException) {
            throw new EdcException(format("Timeout while waiting for catalog by provider %s.", providerUrl),
                    timeoutCatalogFutureGetException);
        }

        Catalog catalog;
        try {
            catalog = new ObjectMapper().readValue(catalogSerialized, Catalog.class);
        } catch (IOException e) {
            throw new EdcException(format("Failed reading catalog of provider %s", providerUrl));
        }

        if (Objects.isNull(catalog)) {
            throw new EdcException(format("Catalog by provider %s couldn't be retrieved", providerUrl));
        }

        // Somehow EDC cannot filter contractOffers by assetId, so fetch all offers and
        // filter locally
        catalog.getContractOffers().removeIf(contractOffer -> !assetId.equals(contractOffer.getAssetId()));

        return catalog.getContractOffers();
    }

    /**
     * Adds an accepted contractOffer to match when checking a provider
     * contractOffer. Only the policies' rules are relevant.
     *
     * @param contractOffers Contract offers whose policies' rules are acceptable
     *                       for an automated contract negotiation
     */
    public void addAccepted(ContractOffer[] contractOffers) {
        contractOfferStore.putOffers(contractOffers);
    }

    /**
     * Return accepted contract offers
     *
     * @return Accepted contract offer list
     */
    public List<ContractOffer> getAccepted() {
        return contractOfferStore.getOffers();
    }

    /**
     * Removes an accepted contractOffer.
     *
     * @param contractOfferId Contract offer id of contract offer to be removed
     */
    public void removeAccepted(String contractOfferId) {
        contractOfferStore.removeOffer(contractOfferId);
    }

    /**
     * Updates an accepted contractOffer.
     *
     * @param contractOfferId Contract offer id of contract offer to be updated
     * @param contractOffer   Updated ContractOffer
     */
    public void updateAccepted(String contractOfferId, ContractOffer contractOffer) {
        contractOfferStore.updateOffer(contractOfferId, contractOffer);
    }

    /**
     * Return contract offers for assetId that match any contractOffers' policy of
     * the services' ContractOfferStore instance containing user added contract
     * offers. If more than one contractOffers are provided by the provider
     * connector, an AmbiguousOrNullException will be thrown.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return One contractOffer offered by the provider for the given assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or otherwise occupied, and was interrupted.
     */
    public ContractOffer getAcceptableContractForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var contractOffers = getContractsForAssetId(providerUrl, assetId);

        if (contractOffers.size() != 1) {
            throw new AmbiguousOrNullException(
                    format("Multiple or no contracts were found for assetId %s! (amount of offers: %s)",
                            assetId, contractOffers.size()));
        }
        ContractOffer contractOffer;
        if (configuration.isAcceptAllProviderOffers() || matchesOwnContractOffers(contractOffers.get(0))) {
            contractOffer = contractOffers.get(0);
        } else {
            throw new EdcException(
                    "Could not find any contract offer matching this connector's accepted contract offers");
        }
        return contractOffer;
    }

    private boolean matchesOwnContractOffers(ContractOffer contractOffer) {
        return contractOfferStore.getOffers().stream().anyMatch(acceptedOffer -> contractOfferRulesEquality(acceptedOffer, contractOffer));
    }

    private boolean contractOfferRulesEquality(ContractOffer first, ContractOffer second) {
        List<Rule> firstRules = new ArrayList<>();
        firstRules.addAll(first.getPolicy().getPermissions());
        firstRules.addAll(first.getPolicy().getProhibitions());
        firstRules.addAll(first.getPolicy().getObligations());

        List<Rule> secondRules = new ArrayList<>();
        secondRules.addAll(second.getPolicy().getPermissions());
        secondRules.addAll(second.getPolicy().getProhibitions());
        secondRules.addAll(second.getPolicy().getObligations());

        return firstRules.stream().anyMatch(firstRule -> secondRules.stream()
                .anyMatch(secondRule -> !ruleEquality(firstRule, secondRule)));
    }

    private <T extends Rule> boolean ruleEquality(T first, T second) {
        return Objects.equals(first.getAction(), second.getAction())
                && Objects.equals(first.getConstraints(), second.getConstraints());
    }
}
