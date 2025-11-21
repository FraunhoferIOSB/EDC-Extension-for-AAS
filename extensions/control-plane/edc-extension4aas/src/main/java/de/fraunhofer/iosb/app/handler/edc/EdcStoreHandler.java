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


public class EdcStoreHandler {

    private final AssetService assetService;
    private final ContractDefinitionService contractDefinitionService;


    public EdcStoreHandler(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore) {
        this.assetService = new AssetService(assetIndex);
        this.contractDefinitionService = new ContractDefinitionService(contractDefinitionStore);
    }


    public StoreResult<Void> register(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> assetCreationResult = assetService.create(asset);

        if (assetCreationResult.succeeded()) {
            return contractDefinitionService.addToContractDefinition(asset.getId(), policyBinding.accessPolicyDefinitionId(),
                    policyBinding.contractPolicyDefinitionId());
        }

        return assetCreationResult;
    }


    public StoreResult<Void> unregister(PolicyBinding policyBinding, String assetId) {
        StoreResult<Asset> assetDeleteResult = assetService.delete(assetId);

        if (assetDeleteResult.succeeded()) {
            return contractDefinitionService.removeFromContract(assetId, policyBinding.accessPolicyDefinitionId(),
                    policyBinding.contractPolicyDefinitionId());
        }

        return StoreResult.generalError(assetDeleteResult.getFailure().getFailureDetail());
    }


    public StoreResult<Asset> update(Asset asset) {
        return assetService.update(asset);
    }

}
