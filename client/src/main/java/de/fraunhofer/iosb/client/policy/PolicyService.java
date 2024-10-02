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
package de.fraunhofer.iosb.client.policy;

import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;


/**
 * Communicates with a provider EDC to retrieve its Catalog.
 * Returns dataset / contract offer for a requested provider asset to the user of this EDC (consumer).
 * Datasets are returned as-is, contract offers are filtered by checking if they are acceptable given
 * the stored acceptable policies.
 */
class PolicyService {

    private final CatalogService catalogService;
    private final TypeTransformerRegistry transformer;

    private final PolicyServiceConfig config;
    private final PolicyDefinitionStore policyDefinitionStore;

    private final JsonLd jsonLdExpander;

    /**
     * Class constructor
     *
     * @param catalogService Fetching the catalog of a provider.
     * @param transformer    Transform json-ld byte-array catalog to catalog class
     */
    PolicyService(CatalogService catalogService, TypeTransformerRegistry transformer,
                  PolicyServiceConfig config, PolicyDefinitionStore policyDefinitionStore, Monitor monitor) {
        this.catalogService = catalogService;
        this.transformer = transformer;
        this.config = config;
        this.policyDefinitionStore = policyDefinitionStore;
        this.jsonLdExpander = new TitaniumJsonLd(monitor);
    }

    ServiceResult<Dataset> getDatasetForAssetId(@NotNull String counterPartyId, @NotNull URL counterPartyUrl,
                                                @NotNull String assetId) {
        var assetQuerySpec = QuerySpec.Builder.newInstance()
                .filter(criterion(Asset.PROPERTY_ID, "=", assetId))
                .build();

        var catalogFuture = catalogService.requestCatalog(
                counterPartyId,
                counterPartyUrl.toString(),
                DATASPACE_PROTOCOL_HTTP,
                assetQuerySpec);

        StatusResult<byte[]> catalogResponse;
        try {
            catalogResponse = catalogFuture.get(config.getWaitForCatalogTimeout(), TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException futureException) {
            return formUnexpectedResult(futureException.getMessage());
        }

        if (catalogResponse.failed()) {
            return formUnexpectedResult(
                    catalogResponse.getFailure().status().name()
                            .concat(": ")
                            .concat(catalogResponse.getFailureDetail()));
        }

        var catalogByteArrayInputStream = new ByteArrayInputStream(catalogResponse.getContent());

        // byte[] -> jsonld -> expanded json
        var catalogJsonObject = Json.createReader(catalogByteArrayInputStream).readObject();
        var catalogExpandedJsonObject = jsonLdExpander.expand(catalogJsonObject);

        if (catalogExpandedJsonObject.failed()) {
            return formUnexpectedResult(catalogExpandedJsonObject.getFailureDetail());
        }

        var catalog = transformer.transform(catalogExpandedJsonObject.getContent(), Catalog.class);

        if (catalog.failed()) {
            return formUnexpectedResult(catalog.getFailureDetail());
        }

        var datasets = catalog.getContent().getDatasets();

        if (datasets == null || datasets.isEmpty()) {
            return ServiceResult.notFound("No datasets were found.");
        } else if (datasets.size() > 1) {
            return ServiceResult.conflict("Multiple datasets were found.");
        }

        return ServiceResult.success(datasets.get(0));
    }

    Result<ContractOffer> getAcceptableContractOfferForAssetId(String counterPartyId, URL providerUrl, String assetId) {
        var datasetResult = getDatasetForAssetId(counterPartyId, providerUrl, assetId);

        if (datasetResult.failed()) {
            return Result.failure(List.of(datasetResult.reason().toString(), datasetResult.getFailureDetail()));
        }

        var acceptablePolicy = datasetResult.getContent()
                .getOffers().entrySet().stream()
                .filter(entry -> config.isAcceptAllProviderOffers() || matchesOwnPolicyDefinitions(entry.getValue()))
                .findAny();

        return acceptablePolicy.map(idPolicyEntry ->
                        Result.success(ContractOffer.Builder.newInstance()
                                .id(idPolicyEntry.getKey())
                                .policy(idPolicyEntry.getValue())
                                .assetId(assetId)
                                .build()))
                .orElse(Result.failure("Could not find acceptable policyDefinition"));
    }

    private ServiceResult<Dataset> formUnexpectedResult(String additionalInformation) {
        var exceptionMessage = Objects.requireNonNullElse(additionalInformation,
                "No error message supplied");
        return ServiceResult.unexpected("Failed fetching catalog", exceptionMessage);
    }

    private boolean matchesOwnPolicyDefinitions(Policy policy) {
        return policyDefinitionStore.getPolicyDefinitions().stream()
                .anyMatch(acceptedPolicyDefinition ->
                        policyDefinitionRulesEquality(
                                acceptedPolicyDefinition.getPolicy(),
                                policy));
    }

    private boolean policyDefinitionRulesEquality(Policy first, Policy second) {
        List<Rule> firstRules = Stream.of(
                        first.getPermissions(),
                        first.getProhibitions(),
                        first.getObligations())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<Rule> secondRules = Stream.of(
                        second.getPermissions(),
                        second.getProhibitions(),
                        second.getObligations())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        return firstRules.stream().allMatch(
                firstRule -> secondRules.stream()
                        .anyMatch(secondRule -> ruleEquality(firstRule, secondRule)));
    }

    private <T extends Rule> boolean ruleEquality(T first, T second) {
        return Objects.equals(first.getAction(), second.getAction()) && Objects.equals(first.getConstraints(),
                second.getConstraints());
    }

}
