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

public class AssetService {

    private final AssetIndex assetIndex;

    public AssetService(AssetIndex assetIndex) {
        this.assetIndex = assetIndex;
    }

    public StoreResult<Void> create(Asset asset) {
        return assetIndex.create(asset);
    }

    public StoreResult<Asset> delete(String assetId) {
        return assetIndex.deleteById(assetId);
    }

    public StoreResult<Asset> update(Asset asset) {
        return assetIndex.updateAsset(asset);
    }
}
