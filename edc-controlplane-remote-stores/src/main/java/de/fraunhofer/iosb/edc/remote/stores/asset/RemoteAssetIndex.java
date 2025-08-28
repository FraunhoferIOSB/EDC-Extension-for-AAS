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
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * AssetIndex implementation where the control plane is reached via http
 */
public class RemoteAssetIndex extends ControlPlaneConnectionHandler implements AssetIndex {

    private RemoteAssetIndex(EdcHttpClient httpClient, Monitor monitor, String protocol, String hostname, int managementPort, String managementPath,
                             ControlPlaneConnection versionConnection, Codec codec, String resourceName) throws IOException {
        super(httpClient, monitor, protocol, hostname, managementPort, managementPath, versionConnection, codec, resourceName);
    }

    private RemoteAssetIndex(EdcHttpClient httpClient, Monitor monitor, String fullManagementUrl, Codec codec, String resourceName) throws MalformedURLException {
        super(httpClient, monitor, fullManagementUrl, codec, resourceName);
    }

    @Override
    public Stream<Asset> queryAssets(QuerySpec querySpec) {
        var querySpecString = codec.serializeQuerySpec(querySpec);

        var request = controlPlane.prepareRequest(HttpMethod.POST, "request", querySpecString);

        var response = executeRequest(request);

        if (!response.succeeded()) {
            monitor.warning(String.format("Failed querying assets: %s", response.getFailureDetail()));
            return Stream.empty();
        }

        var assetListJsonString = response.getContent();

        return codec.deserializeAssets(assetListJsonString).stream();
    }

    @Override
    public Asset findById(String assetId) {
        // Send request
        var request = controlPlane.prepareRequest(HttpMethod.GET, assetId, null);
        // Deserialize response
        var response = executeRequest(request);

        if (response.failed()) {
            monitor.debug(String.format("RemoteAssetIndex.findById failed: %s", response.getFailureDetail()));
            return null;
        }

        var assetJson = response.getContent();


        return codec.deserializeAsset(assetJson);
    }

    /**
     * Extends AssetIndex return values by generalError
     *
     * @param asset The {@link Asset} to store
     * @return Same as AssetIndex, but generalError if connection to control-plane fails
     */
    @Override
    public StoreResult<Void> create(Asset asset) {
        var assetString = codec.serialize(asset);

        var request = controlPlane.prepareRequest(HttpMethod.POST, assetString);

        var response = executeRequest(request);

        if (response.failed()) {
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.CONFLICT) {
                return StoreResult.alreadyExists(response.getFailureDetail());
            }
            return StoreResult.generalError(String.format(UNEXPECTED_ERROR, response.getFailure().getClass().getSimpleName(),
                    response.getFailureDetail()));
        }

        return StoreResult.success();
    }

    @Override
    public StoreResult<Asset> deleteById(String assetId) {
        // NOTE: since deleteById requires the deleted asset as return value and the mgmt-api does not return it, we have to get it first.
        var asset = findById(assetId);

        if (asset == null) {
            return StoreResult.notFound(String.format(ASSET_NOT_FOUND_TEMPLATE, assetId));
        }

        // Send request
        var request = controlPlane.prepareRequest(HttpMethod.DELETE, assetId, null);
        // Deserialize response
        var response = executeRequest(request);

        if (!response.succeeded()) {
            monitor.debug(String.format("RemoteAssetIndex.deleteById failed: %s", response.getFailureDetail()));
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.NOT_FOUND) {
                return StoreResult.notFound(response.getFailureDetail());
            } else if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.CONFLICT) {
                // InMemoryAssetIndex deletes assets regardless, this case is not intended...
                return StoreResult.alreadyLeased(response.getFailureDetail());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailure().getClass().getSimpleName(), response.getFailureDetail()));
        }

        return StoreResult.success(asset);
    }

    @Override
    public long countAssets(List<Criterion> criteria) {
        throw new UnsupportedOperationException("Please request assets via QuerySpec and count them.");
    }

    @Override
    public StoreResult<Asset> updateAsset(Asset asset) {
        var assetString = codec.serialize(asset);

        var request = controlPlane.prepareRequest(HttpMethod.PUT, assetString);

        var response = executeRequest(request);

        if (!response.succeeded()) {
            monitor.debug(String.format("RemoteAssetIndex.updateAsset failed: %s", response.getFailureDetail()));
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.NOT_FOUND) {
                return StoreResult.notFound(response.getFailureDetail());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailure().getClass().getSimpleName(), response.getFailureDetail()));
        }

        return StoreResult.success(findById(asset.getId()));
    }

    @Override
    public DataAddress resolveForAsset(String assetId) {
        var asset = findById(assetId);
        return Optional.ofNullable(asset)
                .map(Asset::getDataAddress)
                .orElse(null);
    }

    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemoteAssetIndex, Builder> {

        public static final String ASSETS = "assets";

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected RemoteAssetIndex create(EdcHttpClient httpClient, Monitor monitor, String protocol, String hostname, int managementPort,
                                          String managementPath, ControlPlaneConnection versionConnection, Codec codec) throws IOException {
            return new RemoteAssetIndex(httpClient, monitor, protocol, hostname, managementPort, managementPath, versionConnection, codec, ASSETS);
        }

        @Override
        protected RemoteAssetIndex create(EdcHttpClient httpClient, Monitor monitor, String fullManagementUrl, Codec codec) throws MalformedURLException {
            return new RemoteAssetIndex(httpClient, monitor, fullManagementUrl, codec, ASSETS);
        }
    }
}
