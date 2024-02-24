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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import de.fraunhofer.iosb.client.exception.AmbiguousOrNullException;
import de.fraunhofer.iosb.client.util.Pair;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.TitaniumJsonLd;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.*;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import de.fraunhofer.iosb.client.exception.AmbiguousOrNullException;
import de.fraunhofer.iosb.client.util.Pair;
import jakarta.json.Json;

/**
 * Finds out policy for a given asset id and provider EDC url
 */
class PolicyService {

    private static final String CATALOG_RETRIEVAL_FAILURE_MSG = "Catalog by provider %s couldn't be retrieved: %s";
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
    public PolicyService(CatalogService catalogService, TypeTransformerRegistry transformer,
            PolicyServiceConfig config, PolicyDefinitionStore policyDefinitionStore, Monitor monitor) {
        this.catalogService = catalogService;
        this.transformer = transformer;

        this.config = config;
        this.policyDefinitionStore = policyDefinitionStore;

        this.jsonLdExpander = new TitaniumJsonLd(monitor);

    }

    Dataset getDatasetForAssetId(String counterPartyId, URL counterPartyUrl, String assetId) throws InterruptedException {
        var catalogFuture = catalogService.requestCatalog(
                counterPartyId, // why do we even need a provider id when we have the url...
                counterPartyUrl.toString(),
                DATASPACE_PROTOCOL_HTTP,
                QuerySpec.Builder.newInstance()
                        .filter(List.of(criterion(Asset.PROPERTY_ID, "=", assetId)))
                        .build());

        StatusResult<byte[]> catalogResponse;
        try {
            catalogResponse = catalogFuture.get(config.getWaitForCatalogTimeout(), TimeUnit.SECONDS);
        } catch (ExecutionException futureExecutionException) {
            throw new EdcException(format("Failed fetching a catalog by provider %s.", counterPartyUrl),
                    futureExecutionException);
        } catch (TimeoutException timeoutCatalogFutureGetException) {
            throw new EdcException(format("Timeout while waiting for catalog by provider %s.", counterPartyUrl),
                    timeoutCatalogFutureGetException);
        }

        if (catalogResponse.failed()) {
            throw new EdcException(format(CATALOG_RETRIEVAL_FAILURE_MSG, counterPartyUrl,
                    catalogResponse.getFailureMessages()));
        }

        var catalogJson = Json.createReader(new ByteArrayInputStream(catalogResponse.getContent()))
                .readObject();

        var catalogJsonExpansionResult = jsonLdExpander.expand(catalogJson);

        if (catalogJsonExpansionResult.failed()) {
            throw new EdcException(format(CATALOG_RETRIEVAL_FAILURE_MSG, counterPartyUrl,
                    catalogJsonExpansionResult.getFailureMessages()));
        }

        var catalogResult = transformer.transform(catalogJsonExpansionResult.getContent(), Catalog.class);

        if (catalogResult.failed()) {
            throw new EdcException(format(CATALOG_RETRIEVAL_FAILURE_MSG, counterPartyUrl,
                    catalogResult.getFailureMessages()));
        }

        var datasets = catalogResult.getContent().getDatasets();
        if (Objects.isNull(datasets) || datasets.size() != 1) {
            throw new AmbiguousOrNullException(
                    format("Multiple or no policyDefinitions were found for assetId %s!",
                            assetId));
        }

        return datasets.get(0);
    }

    Pair<String, Policy> getAcceptablePolicyForAssetId(String counterPartyId, URL providerUrl, String assetId)
            throws InterruptedException {
        var dataset = getDatasetForAssetId(counterPartyId, providerUrl, assetId);

        Map.Entry<String, Policy> acceptablePolicy;
        if (config.isAcceptAllProviderOffers()) {
            acceptablePolicy = dataset.getOffers().entrySet().stream()
                    .findAny()
                    .orElseThrow();

        } else if (dataset.getOffers().values().stream().anyMatch(this::matchesOwnPolicyDefinitions)) {
            acceptablePolicy = dataset.getOffers().entrySet().stream()
                    .filter(entry -> matchesOwnPolicyDefinitions(entry.getValue()))
                    .findAny()
                    .orElseThrow();

        } else {
            throw new EdcException("Could not find any acceptable policyDefinition");
        }

        return new Pair.PairBuilder<String, Policy>()
                .first(acceptablePolicy.getKey()).second(acceptablePolicy.getValue()).build();
    }

    private boolean matchesOwnPolicyDefinitions(Policy policy) {
        return policyDefinitionStore.getPolicyDefinitions().stream().anyMatch(
                acceptedPolicyDefinition -> policyDefinitionRulesEquality(
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

        return firstRules.stream().anyMatch(
                firstRule -> secondRules.stream()
                        .anyMatch(secondRule -> !ruleEquality(firstRule, secondRule)));
    }

    private <T extends Rule> boolean ruleEquality(T first, T second) {
        return Objects.equals(first.getAction(), second.getAction()) && Objects.equals(first.getConstraints(),
                second.getConstraints());
    }

}
