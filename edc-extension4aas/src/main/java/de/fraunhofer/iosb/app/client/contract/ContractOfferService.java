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
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.domain.asset.Asset;

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
 * Finds out policy for a given asset id and provider EDC url
 */
public class ContractOfferService {

    private static final Configuration configuration = Configuration.getInstance();

    private final CatalogService catalogService;
    private final PolicyDefinitionStore policyDefinitionStore;

    /**
     * Class constructor
     *
     * @param catalogService Fetching the catalog of a provider.
     */
    public ContractOfferService(CatalogService catalogService) {
        this.catalogService = catalogService;
        this.policyDefinitionStore = new PolicyDefinitionStore();
    }

    /**
     * Returns datasets for this asset by this provider.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return A list of datasets offered by the provider for the given
     * assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was interrupted.
     */
    public List<Dataset> getDatasetsForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var catalogFuture = catalogService
                .request(providerUrl.toString(),
                        "ids-multipart",
                        QuerySpec.Builder
                                .newInstance()
                                .filter(List.of(new Criterion(Asset.PROPERTY_ID, "=", assetId)))
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

        return new ArrayList<>(catalog.getDatasets());
    }

    /**
     * Return policyDefinition for assetId that match any policyDefinitions' policy of
     * the services' policyDefinitionStore instance containing user added policyDefinitions.
     * If more than one policyDefinitions are provided by the provider
     * connector, an AmbiguousOrNullException will be thrown.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return One policyDefinition offered by the provider for the given assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or otherwise occupied, and was interrupted.
     */
    public Policy getAcceptablePolicyForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var datasets = getDatasetsForAssetId(providerUrl, assetId);

        if (datasets.size() != 1) {
            throw new AmbiguousOrNullException(
                    format("Multiple or no policyDefinitions were found for assetId %s! (amount of policyDefinitions: %s)",
                            assetId, datasets.size()));
        }

        if (configuration.isAcceptAllProviderOffers() || datasets.get(0).getOffers().values().stream().anyMatch(this::matchesOwnPolicyDefinitions)) {
            return datasets.get(0).getOffers().values().stream().filter(this::matchesOwnPolicyDefinitions).findAny().orElse(null);
        } else {
            throw new EdcException("Could not find any contract policyDefinition matching this connector's accepted policyDefinitions");
        }
    }


    /**
     * Adds an accepted contractOffer to match when checking a provider
     * contractOffer. Only the policies' rules are relevant.
     *
     * @param policyDefinitions policies' rules that are acceptable for an automated contract negotiation
     */
    public void addAccepted(PolicyDefinition[] policyDefinitions) {
        policyDefinitionStore.putPolicyDefinitions(policyDefinitions);
    }

    /**
     * Return accepted policyDefinitions
     *
     * @return Accepted policyDefinitions list
     */
    public List<PolicyDefinition> getAccepted() {
        return policyDefinitionStore.getPolicyDefinitions();
    }

    /**
     * Removes an accepted policyDefinitions.
     *
     * @param policyDefinitions policyDefinition id of policyDefinition to be removed
     */
    public void removeAccepted(String policyDefinitions) {
        policyDefinitionStore.removePolicyDefinition(policyDefinitions);
    }

    /**
     * Updates an accepted policyDefinition.
     *
     * @param policyDefinitionId PolicyDefinition id of policyDefinition to be updated
     * @param policyDefinition   Updated PolicyDefinition
     */
    public void updateAccepted(String policyDefinitionId, PolicyDefinition policyDefinition) {
        policyDefinitionStore.updatePolicyDefinitions(policyDefinitionId, policyDefinition);
    }


    private boolean matchesOwnPolicyDefinitions(Policy policy) {
        return policyDefinitionStore.getPolicyDefinitions().stream().anyMatch(acceptedPolicyDefinition -> policyDefinitionRulesEquality(acceptedPolicyDefinition.getPolicy(), policy));
    }

    private boolean policyDefinitionRulesEquality(Policy first, Policy second) {
        List<Rule> firstRules = new ArrayList<>();
        firstRules.addAll(first.getPermissions());
        firstRules.addAll(first.getProhibitions());
        firstRules.addAll(first.getObligations());

        List<Rule> secondRules = new ArrayList<>();
        secondRules.addAll(second.getPermissions());
        secondRules.addAll(second.getProhibitions());
        secondRules.addAll(second.getObligations());

        return firstRules.stream().anyMatch(firstRule -> secondRules.stream()
                .anyMatch(secondRule -> !ruleEquality(firstRule, secondRule)));
    }

    private <T extends Rule> boolean ruleEquality(T first, T second) {
        return Objects.equals(first.getAction(), second.getAction())
                && Objects.equals(first.getConstraints(), second.getConstraints());
    }
}
