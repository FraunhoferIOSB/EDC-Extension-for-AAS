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
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.sync.ChangeSet;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.List;

/**
 * Adds / Removes assets given a ChangeSet of Assets and AssetIDs. Passes the assetIDs on to the next pipelineStep.
 */
public class AssetRegistrar extends PipelineStep<ChangeSet<Asset, String>, ChangeSet<String, String>> {

    private final AssetIndex assetIndex;
    private final Monitor monitor;

    public AssetRegistrar(AssetIndex assetIndex, Monitor monitor) {
        this.assetIndex = assetIndex;
        this.monitor = monitor;
    }

    /**
     * Adds/Removes assets from the given ChangeSet.
     *
     * @param changeSet Assets to add/remove.
     * @return Asset IDs of all the added/removed assets
     */
    @Override
    public PipelineResult<ChangeSet<String, String>> execute(ChangeSet<Asset, String> changeSet) {
        var added = changeSet.toAdd().stream().map(this::createAsset).toList();
        var removed = changeSet.toRemove().stream().map(this::removeAsset).toList();

        var changeSetIds = new ChangeSet.Builder<String, String>()
                .add(added.stream().filter(AbstractResult::succeeded).map(AbstractResult::getContent).toList())
                .remove(removed.stream().filter(AbstractResult::succeeded).map(AbstractResult::getContent).toList()).build();

        if (added.stream().anyMatch(AbstractResult::failed) || removed.stream().anyMatch(AbstractResult::failed)) {
            return PipelineResult.recoverableFailure(changeSetIds, new PipelineFailure(added.stream().filter(AbstractResult::failed).map(AbstractResult::getFailureMessages).flatMap(List::stream).toList(), PipelineFailure.PipelineFailureType.WARNING));
        } else {
            monitor.debug("Added %s, removed %s assets".formatted(changeSetIds.toAdd().size(), changeSetIds.toRemove().size()));
        }

        return PipelineResult.success(changeSetIds);
    }

    private PipelineResult<String> createAsset(Asset asset) {
        var storeResult = assetIndex.create(asset);
        if (storeResult.succeeded()) {
            return PipelineResult.success(asset.getId());
        }
        return PipelineResult.failure(new PipelineFailure(storeResult.getFailure().getMessages(), PipelineFailure.PipelineFailureType.WARNING));
    }

    private PipelineResult<String> removeAsset(String assetId) {
        var storeResult = assetIndex.deleteById(assetId);
        if (storeResult.succeeded()) {
            return PipelineResult.success(assetId);
        }
        return PipelineResult.failure(new PipelineFailure(storeResult.getFailure().getMessages(), PipelineFailure.PipelineFailureType.WARNING));
    }
}