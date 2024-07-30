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

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * ChangeSet is independent of AAS services.
 * Synchronizer shrunk down to just calculate changes in old and new assets
 */
public class Synchronizer extends PipelineStep<Map<Asset, Asset>, ChangeSet<Asset, String>> {

    public Synchronizer() {
    }


    @Override
    public ChangeSet<Asset, String> execute(Map<Asset, Asset> oldAndNewAssets) throws Exception {
        List<String> toRemove = new ArrayList<>();
        List<Asset> toAdd = new ArrayList<>();

        for (var entry : oldAndNewAssets.entrySet()) {
            var oldEnvironment = AssetUtil.flatMapAssets(entry.getKey());
            var newEnvironment = AssetUtil.flatMapAssets(entry.getValue());

            toRemove.addAll(oldEnvironment.stream().filter(oldElement -> !newEnvironment.contains(oldElement)).map(Asset::getId).toList());
            toAdd.addAll(newEnvironment.stream().filter(newElement -> !oldEnvironment.contains(newElement)).toList());
        }

        return new ChangeSet.Builder<Asset, String>().add(toAdd).remove(toRemove).build();
    }
}
