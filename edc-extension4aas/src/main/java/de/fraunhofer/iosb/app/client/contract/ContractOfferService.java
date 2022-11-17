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

import static java.lang.String.format;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;

import de.fraunhofer.iosb.app.client.exception.AmbiguousOrNullException;

/**
 * Finds out contract offer for a given asset id and provider EDC url
 */
public class ContractOfferService {

    // private static final String ASSET_PROPERTY_ID = "asset:prop:id";

    private final CatalogService catalogService;

    /**
     * Class constructor
     * 
     * @param catalogService Fetching the catalog of a provider.
     */
    public ContractOfferService(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    /**
     * Returns an offered contractOffer for this asset by this provider. If zero or
     * multiple contractOffers are found, an {@link AmbiguousOrNullException} is
     * thrown
     * 
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return The contractOffer offered by the provider for the given assetId.
     * @throws ExecutionException
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was interrupted.
     */
    public List<ContractOffer> getContractForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var catalogFuture = catalogService
                .getByProviderUrl(providerUrl.toString(),
                        QuerySpec.Builder
                                .newInstance()
                                // .filter(List.of(new Criterion(ASSET_PROPERTY_ID, "=", assetId)))
                                .build());
        Catalog catalog;
        try {
            catalog = catalogFuture.get();
        } catch (ExecutionException futureExecutionException) {
            throw new EdcException(format("Failed fetching a catalog by provider %s.", providerUrl),
                    futureExecutionException);
        }

        if (Objects.isNull(catalog)) {
            throw new EdcException(format("Catalog by provider %s is null", providerUrl));
        }
        // Somehow EDC cannot filter contractOffers by assetId, so fetch all offers and
        // filter locally
        catalog.getContractOffers().removeIf(contractOffer -> !assetId.equals(contractOffer.getAsset().getId()));

        var contractOffers = catalog.getContractOffers();

        return contractOffers;
    }
}
