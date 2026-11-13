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
package de.fraunhofer.iosb.ilt.dataspace.client.policy;

import de.fraunhofer.iosb.ilt.dataspace.client.config.CatalogConfiguration;
import de.fraunhofer.iosb.ilt.dataspace.client.store.CounterPartyAddressStore;
import jakarta.json.Json;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectConflictException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;
import static org.eclipse.edc.spi.query.Criterion.criterion;


/**
 * Communicates with a provider EDC to retrieve its Catalog. Returns dataset / contract offer for a requested provider
 * asset to the user of this EDC (consumer). Datasets are
 * returned as-is, contract offers are filtered by checking if they are acceptable given the stored acceptable policies.
 */
public class PolicyService {

    private final CatalogService catalogService;
    private final JsonLd jsonLd;
    private final TypeTransformerRegistry transformer;

    private final ParticipantContextStore participantContextStore;
    private final CatalogConfiguration configuration;
    private final AcceptedPolicyStore acceptedPolicyStore;

    private final CounterPartyAddressStore counterPartyAddressStore;

    /**
     * Class constructor
     *
     * @param catalogService Fetching the catalog of a provider.
     * @param transformer Transform json-ld byte-array catalog to catalog class
     */
    public PolicyService(CatalogService catalogService, ParticipantContextStore participantContextStore,
                         TypeTransformerRegistry transformer,
                         CatalogConfiguration configuration, AcceptedPolicyStore acceptedPolicyStore, JsonLd jsonLd, CounterPartyAddressStore counterPartyAddressStore) {
        this.catalogService = catalogService;
        this.participantContextStore = participantContextStore;
        this.transformer = transformer;
        this.configuration = configuration;
        this.acceptedPolicyStore = acceptedPolicyStore;
        this.jsonLd = jsonLd;
        this.counterPartyAddressStore = counterPartyAddressStore;
    }


    public ServiceResult<ContractOffer> getDataset(String participantContextId, String counterPartyId, String counterPartyUri, String assetId) {
        var datasetResult = getDatasetForAssetId(participantContextId, counterPartyId,
                ofNullable(counterPartyUri).orElse(counterPartyAddressStore.getByCounterPartyId(counterPartyId)),
                assetId);

        if (datasetResult.failed()) {
            return switch (datasetResult.reason()) {
                case NOT_FOUND -> throw new ObjectNotFoundException(ContractOffer.class, datasetResult.getFailureDetail());
                case GENERAL_ERROR -> throw new InvalidRequestException(datasetResult.getFailureDetail());
                case DUPLICATE_KEYS -> throw new ObjectConflictException(datasetResult.getFailureDetail());
                default -> ServiceResult.fromFailure(datasetResult);
            };
        }

        var acceptablePolicy = datasetResult.getContent()
                .getOffers().entrySet().stream()
                .filter(entry -> configuration.acceptAllPolicies() || matchesOwnPolicyDefinitions(entry.getValue()))
                .findAny();

        return acceptablePolicy.map(idPolicyEntry -> ServiceResult.success(ContractOffer.Builder.newInstance()
                .id(idPolicyEntry.getKey())
                .policy(idPolicyEntry.getValue().withTarget(assetId))
                .assetId(assetId)
                .build()))
                .orElse(ServiceResult.notFound("Could not find acceptable policyDefinition"));
    }


    private StoreResult<Dataset> getDatasetForAssetId(String participantContextId, @NotNull String counterPartyId, @NotNull String counterPartyUri,
                                                      @NotNull String assetId) {
        var assetQuerySpec = QuerySpec.Builder.newInstance()
                .filter(criterion(Asset.PROPERTY_ID, "=", assetId))
                .build();

        var catalogFuture = catalogService.requestCatalog(
                participantContextStore.findById(participantContextId)
                        .orElseThrow(failure -> new EdcException(String.format("Could not find participant %s: %s", participantContextId, failure.getReason()))),
                counterPartyId,
                counterPartyUri,
                DATASPACE_PROTOCOL_HTTP_V_2025_1,
                assetQuerySpec);

        StatusResult<byte[]> catalogResponse;
        try {
            catalogResponse = catalogFuture.get(configuration.getCatalogTimeout(), TimeUnit.SECONDS);
        }
        catch (ExecutionException | InterruptedException | TimeoutException futureException) {
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
        var catalogExpandedJsonObject = jsonLd.expand(catalogJsonObject);

        if (catalogExpandedJsonObject.failed()) {
            return formUnexpectedResult(catalogExpandedJsonObject.getFailureDetail());
        }

        var catalog = transformer.transform(catalogExpandedJsonObject.getContent(), Catalog.class);

        if (catalog.failed()) {
            return formUnexpectedResult(catalog.getFailureDetail());
        }

        var datasets = catalog.getContent().getDatasets();

        if (datasets == null || datasets.isEmpty()) {
            return StoreResult.notFound(String.format("No datasets were found for asset %s.", assetId));
        }
        else if (datasets.size() > 1) {
            return StoreResult.duplicateKeys(String.format("Multiple datasets were found for asset %s.", assetId));
        }

        return StoreResult.success(datasets.get(0));
    }


    private StoreResult<Dataset> formUnexpectedResult(String additionalInformation) {
        var exceptionMessage = Objects.requireNonNullElse(additionalInformation,
                "No error message supplied");
        return StoreResult.generalError(String.format("Failed fetching catalog: %s", exceptionMessage));
    }


    private boolean matchesOwnPolicyDefinitions(Policy testPolicy) {
        return acceptedPolicyStore.getPolicies().stream()
                .anyMatch(testPolicy::equals);
    }
}
