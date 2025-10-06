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

import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Collection;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Adds and removes assets given a change set of assets and asset IDs.
 * Passes the asset IDs of both the added and removed on to the next pipeline step.
 */
public class AssetRegistrar extends PipelineStep<ChangeSet<Asset, String>, ChangeSet<Asset, Asset>> {

    private final AssetIndex assetIndex;
    private final Monitor monitor;

    /**
     * Class constructor
     *
     * @param assetIndex Add / remove assets from the EDC asset index.
     * @param monitor    Debug log message on how many assets were added/removed (only if >0)
     */
    public AssetRegistrar(AssetIndex assetIndex, Monitor monitor) {
        this.assetIndex = assetIndex;
        this.monitor = monitor;
    }

    /**
     * Adds/Removes assets given the change set.
     *
     * @param changeSet Assets to add/remove.
     * @return Asset IDs of all the added/removed assets
     */
    @Override
    public PipelineResult<ChangeSet<Asset, Asset>> apply(ChangeSet<Asset, String> changeSet) {
        var added = changeSet.toAdd().stream().map(this::create).toList();

        var removed = changeSet.toRemove().stream().map(this::remove).toList();

        // Add contracts for successfully added assets
        var changeSetIds = new ChangeSet.Builder<Asset, Asset>()
                .add(added.stream()
                        .filter(assetIdResultPair ->
                                assetIdResultPair.second().succeeded() ||
                                        ALREADY_EXISTS.equals(assetIdResultPair.second().reason())
                        )
                        .map(Pair::first)
                        .toList())
                .remove(removed.stream()
                        .filter(StoreResult::succeeded)
                        .map(AbstractResult::getContent)
                        .toList())
                .build();

        if (!changeSetIds.toAdd().isEmpty() || !changeSetIds.toRemove().isEmpty()) {
            monitor.info("Added %s, removed %s assets".formatted(changeSetIds.toAdd().size(),
                    changeSetIds.toRemove().size()));
        }

        var addFailureMessages = added.stream()
                .map(Pair::second)
                .filter(AbstractResult::failed)
                // Don't list "already exists" as warning.
                .filter(voidStoreResult -> !ALREADY_EXISTS.equals(voidStoreResult.reason()))
                .map(AbstractResult::getFailureDetail)
                .toList();

        var removeFailureMessages = removed.stream()
                .filter(AbstractResult::failed)
                .filter(result -> !NOT_FOUND.equals(result.reason()))
                .map(AbstractResult::getFailureDetail)
                .toList();

        if (!(addFailureMessages.isEmpty()  && removeFailureMessages.isEmpty())) {
            return PipelineResult.recoverableFailure(changeSetIds,
                    PipelineFailure.warning(Stream.of(addFailureMessages, removeFailureMessages)
                            .flatMap(Collection::stream)
                            .toList()));
        }

        return PipelineResult.success(changeSetIds);
    }

    private Pair<Asset, StoreResult<Void>> create(Asset asset) {
        return new Pair<>(asset, assetIndex.create(asset));
    }

    private StoreResult<Asset> remove(String assetId) {
        return assetIndex.deleteById(assetId);
    }
}