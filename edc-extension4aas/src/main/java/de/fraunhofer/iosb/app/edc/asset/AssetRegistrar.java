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

import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.result.StoreFailure;

/**
 * Adds and removes assets given a change set of assets and asset IDs.
 * Passes the asset IDs of both the added and removed on to the next pipeline step.
 */
public class AssetRegistrar {

    private final AssetIndex assetIndex;

    /**
     * Class constructor
     *
     * @param assetIndex Add / remove assets from the EDC asset index.
     */
    public AssetRegistrar(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

    public PipelineResult<String> createAsset(Asset asset) {
        var storeResult = assetIndex.create(asset);
        if (storeResult.failed() && storeResult.getFailure().getReason().equals(StoreFailure.Reason.GENERAL_ERROR)) {
            return PipelineResult.failure(PipelineFailure.warning(storeResult.getFailureMessages()));
        }
        if (storeResult.failed() && storeResult.getFailure().getReason().equals(StoreFailure.Reason.ALREADY_EXISTS)) {
            return PipelineResult.failure(PipelineFailure.warning(storeResult.getFailureMessages())).withContent(asset.getId());
        }
        return PipelineResult.from(storeResult).withContent(asset.getId());
    }

    public PipelineResult<String> removeAsset(String assetId) {
        var storeResult = assetIndex.deleteById(assetId);
        return PipelineResult.from(storeResult).withContent(assetId);
    }
}