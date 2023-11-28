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

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCAT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DCT_SCHEMA;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_PREFIX;
import static org.eclipse.edc.jsonld.spi.Namespaces.DSPACE_SCHEMA;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCAT_ACCESS_SERVICE_ATTRIBUTE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.DCT_FORMAT_ATTRIBUTE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_PREFIX;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.Rule;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import de.fraunhofer.iosb.client.exception.AmbiguousOrNullException;
import de.fraunhofer.iosb.client.util.Pair;
import jakarta.json.Json;
import jakarta.json.JsonObject;

/**
 * Finds out policy for a given asset id and provider EDC url
 */
public class PolicyService {

    private final PolicyServiceConfig config;

    private final CatalogService catalogService;
    private final TypeTransformerRegistry transformer;
    private final PolicyDefinitionStore policyDefinitionStore;

    /**
     * Class constructor
     *
     * @param catalogService Fetching the catalog of a provider.
     * @param transformer    Transform json-ld byte-array catalog to catalog class
     */
    public PolicyService(Monitor monitor, CatalogService catalogService, TypeTransformerRegistry transformer,
            Config systemConfig) {
        this.config = new PolicyServiceConfig(systemConfig);
        this.policyDefinitionStore = new PolicyDefinitionStore(monitor, this.config.getAcceptedPolicyDefinitionsPath());

        this.catalogService = catalogService;
        this.transformer = transformer;

    }

    /**
     * Returns dataset for this asset by this provider.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return A list of dataset offered by the provider for the given
     *         assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was interrupted.
     */
    public Dataset getDatasetForAssetId(URL providerUrl, String assetId) throws InterruptedException {
        var catalogFuture = catalogService.requestCatalog(
                providerUrl.toString(),
                DATASPACE_PROTOCOL_HTTP,
                QuerySpec.Builder.newInstance()
                        .filter(List.of(criterion(Asset.PROPERTY_ID, "=", assetId)))
                        .build());

        StatusResult<byte[]> catalogResponse;
        try {
            catalogResponse = catalogFuture.get(config.getWaitForCatalogTimeout(), TimeUnit.SECONDS);
        } catch (ExecutionException futureExecutionException) {
            throw new EdcException(format("Failed fetching a catalog by provider %s.", providerUrl),
                    futureExecutionException);
        } catch (TimeoutException timeoutCatalogFutureGetException) {
            throw new EdcException(format("Timeout while waiting for catalog by provider %s.", providerUrl),
                    timeoutCatalogFutureGetException);
        }

        if (catalogResponse.failed()) {
            throw new EdcException(format("Catalog by provider %s couldn't be retrieved: %s", providerUrl,
                    catalogResponse.getFailureMessages()));
        }

        JsonObject modifiedCatalogJson;
        try {
            modifiedCatalogJson = modifyCatalogJson(catalogResponse.getContent());
        } catch (IOException except) {
            throw new EdcException(format("Catalog by provider %s couldn't be retrieved: %s", providerUrl, except));
        }

        var catalog = transformer.transform(modifiedCatalogJson, Catalog.class);

        if (catalog.failed()) {
            throw new EdcException(format("Catalog by provider %s couldn't be retrieved: %s", providerUrl,
                    catalog.getFailureMessages()));
        }

        if (Objects.isNull(catalog.getContent().getDatasets()) || catalog.getContent().getDatasets().size() != 1) {
            throw new AmbiguousOrNullException(format("Multiple or no policyDefinitions were found for assetId %s!",
                    assetId));
        }

        var dataset = catalog.getContent().getDatasets().get(0);
        dataset.getDistributions().remove(0); // Remove distribution added to deserialize catalog

        return dataset;
    }

    /**
     * Return policyDefinition for assetId that match any policyDefinitions' policy
     * of
     * the services' policyDefinitionStore instance containing user added
     * policyDefinitions.
     * If more than one policyDefinitions are provided by the provider
     * connector, an AmbiguousOrNullException will be thrown.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return One policyDefinition offered by the provider for the given assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was
     *                              interrupted.
     */
    public Pair<String, Policy> getAcceptablePolicyForAssetId(URL providerUrl, String assetId)
            throws InterruptedException {
        var dataset = getDatasetForAssetId(providerUrl, assetId);

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
            throw new EdcException("Could not find any contract policyDefinition matching this connector's accepted " +
                    "policyDefinitions");
        }

        return new Pair.PairBuilder<String, Policy>()
                .first(acceptablePolicy.getKey()).second(acceptablePolicy.getValue()).build();
    }

    /**
     * Adds an accepted contractOffer to match when checking a provider
     * contractOffer. Only the policies' rules are relevant.
     *
     * @param policyDefinitions policies' rules that are acceptable for an automated
     *                          contract negotiation
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
     * @param policyDefinitions policyDefinition id of policyDefinition to be
     *                          removed
     * @return Optional containing removed policy definition or null
     */
    public Optional<PolicyDefinition> removeAccepted(String policyDefinitions) {
        return policyDefinitionStore.removePolicyDefinition(policyDefinitions);
    }

    /**
     * Updates an accepted policyDefinition.
     *
     * @param policyDefinitionId PolicyDefinition id of policyDefinition to be
     *                           updated
     * @param policyDefinition   Updated PolicyDefinition
     */
    public Optional<PolicyDefinition> updateAccepted(String policyDefinitionId, PolicyDefinition policyDefinition) {
        return policyDefinitionStore.updatePolicyDefinitions(policyDefinitionId, policyDefinition);
    }

    private boolean matchesOwnPolicyDefinitions(Policy policy) {
        return policyDefinitionStore.getPolicyDefinitions().stream().anyMatch(
                acceptedPolicyDefinition -> policyDefinitionRulesEquality(acceptedPolicyDefinition.getPolicy(),
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
                firstRule -> secondRules.stream().anyMatch(secondRule -> !ruleEquality(firstRule, secondRule)));
    }

    private <T extends Rule> boolean ruleEquality(T first, T second) {
        return Objects.equals(first.getAction(), second.getAction()) && Objects.equals(first.getConstraints(),
                second.getConstraints());
    }

    /*
     * Since EDC api does not return Catalog object directly, resort to another
     * solution for now.
     */
    private JsonObject modifyCatalogJson(byte[] catalogBytes) throws IOException {

        // Bytes to string. Replace "dcat:" etc. by schema. String to bytes again
        catalogBytes = new String(catalogBytes)
                .replace(DCAT_PREFIX + ":", DCAT_SCHEMA)
                .replace(ODRL_PREFIX + ":", ODRL_SCHEMA)
                .replace(DCT_PREFIX + ":", DCT_SCHEMA)
                .replace(DSPACE_PREFIX + ":", DSPACE_SCHEMA)
                .getBytes();

        Distribution distribution = Distribution.Builder.newInstance()
                .format("JSON")
                .dataService(DataService.Builder.newInstance().build())
                .build();

        JsonNode jsonNode;
        var om = new ObjectMapper();

        var distributionString = om.writeValueAsString(distribution);
        var distributionNode = om.readTree(distributionString
                .replace("format", DCT_FORMAT_ATTRIBUTE)
                .replace("dataService", DCAT_ACCESS_SERVICE_ATTRIBUTE));

        jsonNode = om.readTree(catalogBytes);

        if (!jsonNode.has(DCAT_SCHEMA + "dataset") || jsonNode.get(DCAT_SCHEMA + "dataset") == null
                || jsonNode.get(DCAT_SCHEMA + "dataset").isEmpty()) {
            throw new EdcException("No dataset provided in catalog.");
        }

        ((ArrayNode) jsonNode
                .get(DCAT_SCHEMA + "dataset")
                .get(DCAT_SCHEMA + "distribution"))
                .add(distributionNode);
        catalogBytes = om.writeValueAsBytes(jsonNode);

        var jsonReader = Json.createReader(new ByteArrayInputStream(catalogBytes));
        return jsonReader.readObject();
    }

}
