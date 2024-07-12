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
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;

/**
 * Controls communication with EDC (EDC contracts and EDC assets)
 */
public class ResourceController {

    private final ResourceHandler resourceAgent;
    private final ContractHandler contractHandler;

    public ResourceController(AssetIndex assetIndex, ContractDefinitionStore contractStore,
                              PolicyDefinitionStore policyStore, Monitor monitor) {
        resourceAgent = new ResourceHandler(assetIndex);
        contractHandler = new ContractHandler(contractStore, policyStore, monitor);
    }

    /**
     * Registers resource at EDC with default contract, return contract id of new
     * asset
     *
     * @param sourceUrl      URL to resource to be registered
     * @param referenceChain To create the full access url dynamically this reference chain is passed
     * @param name           The name of the asset (e.g., idShort)
     * @param contentType    Content behind the sourceUrl
     * @return contract id
     */
    public Pair<String, String> createResource(String sourceUrl, Reference referenceChain, String name, String contentType) {
        var assetId = resourceAgent.createAsset(sourceUrl, referenceChain, name, contentType);
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

}
