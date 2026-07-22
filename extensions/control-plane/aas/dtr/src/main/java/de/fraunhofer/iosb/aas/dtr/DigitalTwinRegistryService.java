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
package de.fraunhofer.iosb.aas.dtr;

import de.fraunhofer.iosb.app.edc.policy.PolicyHelper;
import de.fraunhofer.iosb.codec.Codec;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class DigitalTwinRegistryService {

    private static final String DTR_TAXONOMY_TYPE = "https://w3id.org/catenax/taxonomy#DigitalTwinRegistry";

    private final AssetIndex assetIndex;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final ContractDefinitionStore contractDefinitionStore;

    private final String registryUri;
    private final String specificationVersion;

    private final String accessPolicyDefinitionId;
    private final String usagePolicyDefinitionId;

    private String assetId;
    private String contractDefinitionId;

    public DigitalTwinRegistryService(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore,
            PolicyDefinitionStore policyDefinitionStore, Supplier<String> participantContextId,
            Codec codec, DigitalTwinRegistryExtensionConfiguration configuration) {
        this.assetIndex = assetIndex;
        this.contractDefinitionStore = contractDefinitionStore;
        this.policyDefinitionStore = policyDefinitionStore;

        this.registryUri = configuration.getUrl();
        this.specificationVersion = configuration.getSpecificationVersion();

        this.accessPolicyDefinitionId = Optional.ofNullable(configuration.getAccessPolicyDefinitionId())
                .orElseGet(() -> initializePolicy(participantContextId, codec,
                        configuration.getAccessPolicyPath()));

        this.usagePolicyDefinitionId = Optional.ofNullable(configuration.getUsagePolicyDefinitionId())
                .orElseGet(() -> {
                    // If access policy path is the same as usage policy path, no need to create a
                    // second policy definition.
                    if (configuration.getAccessPolicyDefinitionId() == null
                            && configuration.getAccessPolicyPath().equals(configuration.getUsagePolicyPath())) {
                        return this.accessPolicyDefinitionId;
                    }
                    return initializePolicy(participantContextId, codec,
                            configuration.getUsagePolicyPath());
                });
    }

    public void register() {
        assetIndex.create(buildAsset());
        contractDefinitionStore.save(builContractDefinition());
    }

    public void cleanUp() {
        // Remove asset, policies, contract
        assetIndex.deleteById(assetId);
        contractDefinitionStore.deleteById(contractDefinitionId);
        policyDefinitionStore.delete(accessPolicyDefinitionId);
        if (!accessPolicyDefinitionId.equals(usagePolicyDefinitionId)) {
            policyDefinitionStore.delete(usagePolicyDefinitionId);
        }
    }

    private ContractDefinition builContractDefinition() {
        this.contractDefinitionId = UUID.randomUUID().toString();
        return ContractDefinition.Builder.newInstance()
                .id(this.contractDefinitionId)
                .accessPolicyId(accessPolicyDefinitionId)
                .contractPolicyId(usagePolicyDefinitionId)
                .assetsSelector(
                        List.of(Criterion.criterion(Asset.PROPERTY_ID, CriterionOperatorRegistry.EQUAL, assetId)))
                .build();
    }

    private Asset buildAsset() {
        Asset.Builder assetBuilder = Asset.Builder.newInstance();

        assetBuilder.property("http://purl.org/dc/terms/type", DTR_TAXONOMY_TYPE);
        assetBuilder.property("https://w3id.org/catenax/ontology/common#version", this.specificationVersion);

        this.assetId = UUID.randomUUID().toString();
        assetBuilder.id(this.assetId);

        assetBuilder.dataAddress(buildDataAddress());

        return assetBuilder.build();
    }

    private DataAddress buildDataAddress() {
        return HttpDataAddress.Builder.newInstance()
                .baseUrl(registryUri)
                .build();
    }

    private String initializePolicy(Supplier<String> participantContextId,
            Codec codec, String policyPath) {

        if (policyPath == null) {
            throw new EdcException("DTR Extension requires policy either as path or ID");
        }

        Result<Policy> policyResult = PolicyHelper.getPolicyFromFile(codec, policyPath);

        if (policyResult.failed()) {
            throw new EdcException(String.format("DTR Extension: Policy could not be deserialized from %s: %s",
                    policyPath, policyResult.getFailureDetail()));
        }

        PolicyDefinition policyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(policyResult.getContent())
                .id(UUID.randomUUID().toString())
                .participantContextId(participantContextId.get())
                .build();

        StoreResult<PolicyDefinition> result = policyDefinitionStore.create(policyDefinition);

        if (result.failed()) {
            throw new EdcException(result.getFailureDetail());
        }

        return result.getContent().getId();
    }
}
