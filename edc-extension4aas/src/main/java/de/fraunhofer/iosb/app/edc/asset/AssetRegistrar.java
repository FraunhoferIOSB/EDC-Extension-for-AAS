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

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.sync.ChangeSet;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.EdcException;

/**
 * Internal communication with EDC. Manages EDC assets and contracts.
 */
public class AssetRegistrar extends PipelineStep<ChangeSet<Asset, String>, ChangeSet<String, String>> {

    private final AssetIndex assetIndex;

    public AssetRegistrar(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

    @Override
    public ChangeSet<String, String> execute(ChangeSet<Asset, String> changeSet) throws Exception {
        return new ChangeSet.Builder<String, String>()
                .add(changeSet.toAdd().stream().map(this::createAsset).toList())
                .remove(changeSet.toRemove().stream().map(this::removeAsset).toList()).build();
    }

    /**
     * Registers an asset at the EDC.
     *
     * @param asset The asset
     */
    public String createAsset(Asset asset) {
        var storeResult = assetIndex.create(asset);
        if (storeResult.succeeded()) {
            return asset.getId();
        } else {
            throw new EdcException("Could not create asset: %s. %s"
                    .formatted(asset.getId(), storeResult.getFailure().getMessages()));
        }
    }

    /**
     * Removes asset from assetIndex.
     *
     * @param assetId asset id
     */
    public String removeAsset(String assetId) {
        var storeResult = assetIndex.deleteById(assetId);
        if (storeResult.succeeded()) {
            return assetId;
        } else {
            throw new EdcException("Could not delete asset %s. %s".formatted(assetId, storeResult.getFailure().getMessages()));

        }
    }
}