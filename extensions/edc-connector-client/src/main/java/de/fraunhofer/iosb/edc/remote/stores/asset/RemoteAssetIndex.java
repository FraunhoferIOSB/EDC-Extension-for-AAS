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
package de.fraunhofer.iosb.edc.remote.stores.asset;

import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * AssetIndex implementation where the control plane is reached via http
 */
public class RemoteAssetIndex extends ControlPlaneConnectionHandler<Asset> implements AssetIndex {

    private static final String MGMT_API_RESOURCE_ACCESSOR = "assets";

    private RemoteAssetIndex(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        super(monitor, httpClient, codec, connection);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        return queryEntities(querySpec, Asset.class);
    }

    @Override
    public Asset findById(String assetId) {
        return findById(assetId, Asset.class);
    }

    /**
     * Extends AssetIndex return values by generalError
     *
     * @param asset The {@link Asset} to store
     * @return Same as AssetIndex, but generalError if connection to control-plane fails
     */
    @Override
    public StoreResult<Void> create(Asset asset) {
        return createEntity(asset);
    }


    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        return deleteById(assetId, Asset.class);
    }

    @Override
    public long countAssets(List<Criterion> criteria) {
        QuerySpec querySpec = QuerySpec.Builder.newInstance()
                .filter(criteria)
                .build();

        return queryAssets(querySpec).count();
    }

    @Override
    public StoreResult<Asset> updateAsset(Asset asset) {
        return updateEntity(asset, Asset.class);
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        var asset = findById(assetId);
        return Optional.ofNullable(asset)
                .map(Asset::getDataAddress)
                .orElse(null);
    }

    @Override
    protected String getExistsTemplate() {
        return ASSET_EXISTS_TEMPLATE;
    }


    @Override
    protected String getNotFoundTemplate() {
        return ASSET_NOT_FOUND_TEMPLATE;
    }

    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemoteAssetIndex, Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public RemoteAssetIndex build() {
            this.resourceName = MGMT_API_RESOURCE_ACCESSOR;
            return super.build();
        }

        @Override
        protected RemoteAssetIndex create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
            return new RemoteAssetIndex(monitor, httpClient, codec, connection);
        }
    }
}
