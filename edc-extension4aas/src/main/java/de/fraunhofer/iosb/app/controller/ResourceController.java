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
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Collection;

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
     * Registers resources at EDC with default contract
     *
     * @param assets Assets to be registered
     */
    public void createResources(Collection<Asset> assets) {
        assets.forEach(asset -> {
            resourceAgent.createAsset(asset);
            contractHandler.registerAssetToDefaultContract(asset.getId());
        });
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
