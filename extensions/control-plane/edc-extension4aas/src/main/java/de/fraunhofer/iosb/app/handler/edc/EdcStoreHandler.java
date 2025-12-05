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
package de.fraunhofer.iosb.app.handler.edc;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.edc.asset.AssetService;
import de.fraunhofer.iosb.app.edc.contract.ContractDefinitionService;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.spi.result.StoreResult;


/**
 * Handles transactions with EDC. Makes sure that registering an asset and the asset to a contract is a transaction.
 */
public class EdcStoreHandler {

    private final AssetService assetService;
    private final ContractDefinitionService contractDefinitionService;


    /**
     * Class constructor.
     *
     * @param assetIndex To add/remove assets.
     * @param contractDefinitionStore To add/remove asset ids to/from contracts, to create/remove contracts.
     * @param participantId The participantId of the participant that the AAS extension publishes for.
     */
    public EdcStoreHandler(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore, String participantId) {
        this.assetService = new AssetService(assetIndex, participantId);
        this.contractDefinitionService = new ContractDefinitionService(contractDefinitionStore, participantId);
    }


    /**
     * Register an asset to the EDC AssetIndex and attach a contract with the policyIds from the policyBinding to it before registering said contract to the EDC
     * ContractDefinitionStore.
     *
     * @param policyBinding PolicyBinding containing access and usage (contract) policy ids.
     * @param asset Asset to register to EDC AssetIndex.
     * @return Successful result if all the abovementioned process succeeded, else failure.
     */
    public StoreResult<Void> register(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> assetCreationResult = assetService.create(asset);

        if (assetCreationResult.succeeded()) {
            return contractDefinitionService.addToContractDefinition(asset.getId(), policyBinding.accessPolicyDefinitionId(),
                    policyBinding.contractPolicyDefinitionId());
        }

        return assetCreationResult;
    }


    /**
     * Unregister an asset from the EDC AssetIndex and detach the corresponding contract with the policyIds from the policyBinding to it, optionally unregistering said contract
     * from the EDC ContractDefinitionStore if it is dangling after the detachment.
     *
     * @param policyBinding PolicyBinding containing access and usage (contract) policy ids.
     * @param assetId id of the asset to unregister from EDC AssetIndex.
     * @return Successful result if all the abovementioned process succeeded, else failure.
     */
    public StoreResult<Void> unregister(PolicyBinding policyBinding, String assetId) {
        StoreResult<Asset> assetDeleteResult = assetService.delete(assetId);

        if (assetDeleteResult.succeeded()) {
            return contractDefinitionService.removeFromContract(assetId, policyBinding.accessPolicyDefinitionId(),
                    policyBinding.contractPolicyDefinitionId());
        }

        return StoreResult.generalError(assetDeleteResult.getFailure().getFailureDetail());
    }


    /**
     * Update an asset at the EDC AssetIndex does not alter the ContractStore.
     *
     * @param asset Asset to register to EDC AssetIndex.
     * @return Successful result if all the abovementioned process succeeded, else failure.
     */
    public StoreResult<Asset> update(Asset asset) {
        return assetService.update(asset);
    }
}
