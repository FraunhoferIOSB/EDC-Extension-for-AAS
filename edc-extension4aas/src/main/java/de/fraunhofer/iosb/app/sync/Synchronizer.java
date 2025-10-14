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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.ACCESS_POLICY_FIELD;
import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.CONTRACT_POLICY_FIELD;
import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.SubmodelElementMapper.SMC_CHILDREN_LOCATION;
import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable.SubmodelMapper.SUBMODEL_ELEMENT_LOCATION;

/**
 * A synchronizer gets as input two assets:
 * - The asset holding the currently stored assets for an AAS service
 * and a more current asset built from the AAS environment.
 * The synchronizer now checks which assets got added / removed
 * since the last synchronization and passes them on to the next pipeline step as a ChangeSet
 * consisting of the new assets and the IDs of the assets to be removed.
 */
public class Synchronizer extends PipelineStep<Collection<Pair<Asset, Asset>>, ChangeSet<Asset, String>> {

    /**
     * Receives pairs of AAS environment assets and computes their deltas,
     * i.e. what needs to be removed and added to get from the first environment to the second.
     * The first environment is here the currently stored environment (AssetIndex) and the second is the new one,
     * from the AAS service / registry
     *
     * @param oldAndNewAssets For n AAS services/registries, the currently stored environment and the new one,
     *                        both in the form of assets.
     * @return A changeset containing a collection of assets to be added and a collection of assetIds to be removed.
     */
    @Override
    public PipelineResult<ChangeSet<Asset, String>> apply(Collection<Pair<Asset, Asset>> oldAndNewAssets) {
        if (oldAndNewAssets == null) {
            return PipelineResult.failure(PipelineFailure.warning(List.of("Empty input for synchronizer")));
        }

        Collection<Asset> allCurrentlyStored = oldAndNewAssets.stream()
                .map(Pair::first)
                .filter(Objects::nonNull)
                .map(AssetUtil::flatMapAssets)
                .flatMap(Collection::stream)
                .filter(asset -> null != asset.getPrivateProperty(ACCESS_POLICY_FIELD))
                .filter(asset -> null != asset.getPrivateProperty(CONTRACT_POLICY_FIELD))
                .toList();

        Collection<Asset> allNewAssets = oldAndNewAssets.stream()
                .map(Pair::second)
                .filter(Objects::nonNull)
                .map(AssetUtil::flatMapAssets)
                .flatMap(Collection::stream)
                .filter(asset -> null != asset.getPrivateProperty(ACCESS_POLICY_FIELD))
                .filter(asset -> null != asset.getPrivateProperty(CONTRACT_POLICY_FIELD))
                .toList();

        Collection<String> toRemove =
                new ArrayList<>(allCurrentlyStored.stream()
                        .filter(currentElement -> absent(allNewAssets, currentElement))
                        .map(Asset::getId)
                        .toList());

        Collection<Asset> toAdd = new ArrayList<>(allNewAssets.stream()
                .filter(newElement -> absent(allCurrentlyStored, newElement))
                .toList());


        return PipelineResult.success(new ChangeSet.Builder<Asset, String>().add(toAdd).remove(toRemove).build());
    }

    private boolean absent(Collection<Asset> assets, Asset asset) {
        return assets.stream().noneMatch(a -> assetsEquality(a, asset));
    }

    private boolean assetsEquality(Asset a, Asset b) {
        // Don't check for submodelElements or collectionElements as they are handled on their own
        Set<Map.Entry<String, Object>> aFilteredProperties =
                a.getProperties().entrySet().stream()
                        .filter(entry -> !SUBMODEL_ELEMENT_LOCATION.equals(entry.getKey()))
                        .filter(entry -> !SMC_CHILDREN_LOCATION.equals(entry.getKey()))
                        .collect(Collectors.toSet());
        Set<Map.Entry<String, Object>> bFilteredProperties =
                b.getProperties().entrySet().stream()
                        .filter(entry -> !SUBMODEL_ELEMENT_LOCATION.equals(entry.getKey()))
                        .filter(entry -> !SMC_CHILDREN_LOCATION.equals(entry.getKey()))
                        .collect(Collectors.toSet());

        return Objects.equals(a.getId(), b.getId()) &&
                Objects.equals(aFilteredProperties, bFilteredProperties) &&
                Objects.equals(a.getDataAddress().getProperties(), b.getDataAddress().getProperties()) &&
                Objects.equals(a.getDataAddress().getClass(), b.getDataAddress().getClass()) &&
                Objects.equals(a.getPrivateProperties(), b.getPrivateProperties());
    }
}
