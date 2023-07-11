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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.edc.ContractHandler;
import de.fraunhofer.iosb.app.edc.ResourceHandler;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.asset.AssetIndex;

/**
 * Controls communication with EDC (EDC contracts and EDC assets)
 */
public class ResourceController {

    private final ResourceHandler resourceAgent;
    private final ContractHandler contractHandler;

    public ResourceController(AssetIndex assetIndex, ContractDefinitionStore contractStore,
                              PolicyDefinitionStore policyStore) {
        resourceAgent = new ResourceHandler(assetIndex);
        contractHandler = new ContractHandler(contractStore, policyStore);
    }

    /**
     * Registers resource at EDC with default contract, return contract id of new
     * asset
     *
     * @param linkToResource Link to resource to be registered
     * @param name           The name of the asset (e.g., idShort)
     * @param contentType    Content behind the sourceUrl
     * @param version        For versioning of assets
     * @return contract id
     */
    public Pair<String, String> createResource(String linkToResource, String name, String contentType, String version) {
        var assetId = resourceAgent.createAsset(linkToResource, name, contentType, version);
        var contractId = contractHandler.registerAssetToDefaultContract(assetId);
        return new Pair<>(assetId, contractId);
    }

    /**
     * Disassociates asset from all contracts, then delete it from EDC AssetIndex.
     *
     * @param assetId Asset ID of asset to be deleted
     */
    public void deleteAssetAndContracts(String assetId) {
        contractHandler.deleteContractsWithAssetId(assetId);
        resourceAgent.deleteAsset(assetId);
    }

    /**
     * Removes a contract
     *
     * @param contractId The contract id
     */
    public void deleteContract(String contractId) {
        contractHandler.deleteContractDefinition(contractId);
    }
}
