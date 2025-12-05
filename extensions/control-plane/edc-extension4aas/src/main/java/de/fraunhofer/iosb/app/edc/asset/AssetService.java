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
package de.fraunhofer.iosb.app.edc.asset;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.result.StoreResult;


/**
 * Persists assets to, deletes assets by ID from and updates assets at the EDC asset store (AssetIndex).
 */
public class AssetService {

    private final AssetIndex assetIndex;
    private final String participantId;


    /**
     * Class constructor.
     *
     * @param assetIndex The edc's asset store.
     * @param participantId Participant ID under which AAS extension registers data in data space
     */
    public AssetService(AssetIndex assetIndex, String participantId) {
        this.assetIndex = assetIndex;
        this.participantId = participantId;
    }


    /**
     * Persists this asset under the EDC's asset store. Also attaches the participantId to the asset!
     *
     * @param asset The asset to persist.
     * @return Whether the operation succeeded or failed.
     */
    public StoreResult<Void> create(Asset asset) {
        return assetIndex.create(asset.toBuilder().participantContextId(participantId).build());
    }


    /**
     * Removes the asset - defined by the assetId - from the EDC's asset store.
     *
     * @param assetId The assetId of the asset to remove.
     * @return Whether the operation succeeded or failed.
     */
    public StoreResult<Asset> delete(String assetId) {
        return assetIndex.deleteById(assetId);
    }


    /**
     * Persists an updated asset under the EDC's asset store. Also attaches the participantId to the asset!
     *
     * @param asset The asset to update.
     * @return Whether the operation succeeded or failed.
     */
    public StoreResult<Asset> update(Asset asset) {
        return assetIndex.updateAsset(asset.toBuilder().participantContextId(participantId).build());
    }
}
