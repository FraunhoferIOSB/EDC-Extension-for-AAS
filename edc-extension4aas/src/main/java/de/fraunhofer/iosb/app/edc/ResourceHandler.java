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
package de.fraunhofer.iosb.app.edc;

import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.types.domain.HttpDataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.asset.Asset;

/**
 * Internal communication with EDC. Manages EDC assets and contracts.
 */
public class ResourceHandler {

    private final AssetLoader assetLoader;

    public ResourceHandler(AssetLoader assetLoader) {
        this.assetLoader = assetLoader;
    }

    /**
     * Registers an asset at the EDC.

     * @param sourceUrl Data of the asset
     * @return asset ID of created asset
     */
    public String createAsset(String sourceUrl) {
        final var assetId = createAssetId(sourceUrl);
        final var dataAddress = HttpDataAddress.Builder.newInstance().baseUrl(sourceUrl).build();
        final var asset = Asset.Builder.newInstance().id(assetId.toString()).build();
        assetLoader.accept(asset, dataAddress);
        return assetId;
    }

    /**
     * Removes asset from assetIndex.

     * @param assetId asset id
     */
    public void deleteAsset(String assetId) {
        assetLoader.deleteById(assetId);
    }

    private String createAssetId(String sourceUrl) {
        return String.valueOf(sourceUrl.hashCode());
    }
}
