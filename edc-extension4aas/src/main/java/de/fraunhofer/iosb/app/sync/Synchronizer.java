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
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;


/**
 * A synchronizer gets as input two assets:
 * - The asset holding the currently stored assets for an AAS service
 * and a more current asset built from the AAS environment.
 * The synchronizer now checks which assets got added / removed
 * since the last synchronization and passes them on to the next pipeline step as a ChangeSet
 * consisting of the new assets and the IDs of the assets to be removed.
 */
public class Synchronizer extends PipelineStep<Collection<Pair<Asset, Asset>>, ChangeSet<Asset, String>> {

    public Synchronizer() {
    }

    /**
     * Finds out which assets are to be removed / added based on a more current environment from the AAS.
     *
     * @param oldAndNewAssets For n AAS services, the currently stored environment
     *                        and the new one, both in the form of assets.
     * @return For the n AAS services the asset containing assets to be added and the assetIDs of the assets to be removed
     */
    @Override
    public PipelineResult<ChangeSet<Asset, String>> apply(Collection<Pair<Asset, Asset>> oldAndNewAssets) {
        Collection<String> toRemove = new ArrayList<>();
        Collection<Asset> toAdd = new ArrayList<>();

        for (var entry : oldAndNewAssets) {
            var oldEnvironment = entry.first() != null ? AssetUtil.flatMapAssets(entry.first()) : new ArrayList<Asset>();
            // New environment cannot be null.
            var newEnvironment = AssetUtil.flatMapAssets(entry.second());


            toRemove.addAll(oldEnvironment.stream().filter(oldElement -> absent(newEnvironment, oldElement)).map(Asset::getId).toList());
            toAdd.addAll(newEnvironment.stream().filter(newElement -> absent(oldEnvironment, newElement)).toList());
        }

        return PipelineResult.success(new ChangeSet.Builder<Asset, String>().add(toAdd).remove(toRemove).build());
    }

    private boolean absent(Collection<Asset> assets, Asset asset) {
        return assets.stream().noneMatch(a -> a.getId().equals(asset.getId()));
    }
}
