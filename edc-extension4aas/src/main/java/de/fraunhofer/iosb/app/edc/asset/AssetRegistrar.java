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
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.SubmodelElementMapper.SMC_CHILDREN_LOCATION;
import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable.IdentifiableMapper.SUBMODEL_ELEMENT_LOCATION;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

/**
 * Adds and removes assets given a change set of assets and asset IDs.
 * Passes the asset IDs of both the added and removed on to the next pipeline step.
 */
public class AssetRegistrar extends PipelineStep<ChangeSet<Asset, String>, ChangeSet<Asset, Asset>> {


    private final List<String> propertiesToFilter = List.of(SUBMODEL_ELEMENT_LOCATION, SMC_CHILDREN_LOCATION);
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
        var removed = changeSet.toRemove().stream().map(this::remove).toList();
        var added = changeSet.toAdd().stream().map(this::create).toList();

        // Add contracts for successfully added assets
        var changeSetIds = new ChangeSet.Builder<Asset, Asset>()
                .add(added.stream()
                        .filter(assetIdResultPair ->
                                assetIdResultPair.second().succeeded() ||
                                        ALREADY_EXISTS == assetIdResultPair.second().reason()
                        )
                        .map(Pair::first)
                        .toList())
                .remove(removed.stream()
                        .filter(StoreResult::succeeded)
                        .map(AbstractResult::getContent)
                        .toList())
                .build();

        if (!changeSetIds.toAdd().isEmpty() || !changeSetIds.toRemove().isEmpty()) {
            long numberAdded = added.stream()
                    .map(Pair::second)
                    .filter(AbstractResult::succeeded).count();
            long numberRemoved = removed.stream()
                    .filter(AbstractResult::succeeded).count();

            monitor.info("Added %s, removed %s assets".formatted(numberAdded, numberRemoved));
        }

        if (!changeSetIds.toAdd().isEmpty() && added.stream()
                .map(Pair::second)
                .filter(AbstractResult::failed)
                .map(StoreResult::reason)
                .anyMatch(reason -> ALREADY_EXISTS == reason)) {

            long numberExisted = added.stream()
                    .map(Pair::second)
                    .filter(AbstractResult::failed)
                    .map(StoreResult::reason)
                    .filter(ALREADY_EXISTS::equals).count();

            monitor.info("%s assets existed".formatted(numberExisted));
        }

        var addFailureMessages = added.stream()
                .map(Pair::second)
                .filter(AbstractResult::failed)
                // Don't list "already exists" as warning.
                .filter(voidStoreResult -> ALREADY_EXISTS != voidStoreResult.reason())
                .map(AbstractResult::getFailureDetail)
                .toList();

        var removeFailureMessages = removed.stream()
                .filter(AbstractResult::failed)
                .filter(result -> NOT_FOUND != result.reason())
                .map(AbstractResult::getFailureDetail)
                .toList();

        if (!(addFailureMessages.isEmpty() && removeFailureMessages.isEmpty())) {
            return PipelineResult.recoverableFailure(changeSetIds,
                    PipelineFailure.warning(Stream.of(addFailureMessages, removeFailureMessages)
                            .flatMap(Collection::stream)
                            .toList()));
        }

        return PipelineResult.success(changeSetIds);
    }

    private Pair<Asset, StoreResult<Void>> create(Asset asset) {
        var updatedProperties = new HashMap<>(asset.getProperties());

        propertiesToFilter.forEach(updatedProperties::remove);

        Asset updatedAsset = Asset.Builder.newInstance()
                .id(asset.getId())
                .createdAt(asset.getCreatedAt())
                .dataAddress(asset.getDataAddress())
                .properties(updatedProperties)
                .privateProperties(asset.getPrivateProperties())
                .build();

        StoreResult<Void> createResult = assetIndex.create(updatedAsset);

        // Update if exists
        if (createResult.failed() && ALREADY_EXISTS == createResult.reason()) {
            StoreResult<Asset> updateResult = assetIndex.updateAsset(updatedAsset);
            if (updateResult.failed()) {
                return new Pair<>(updatedAsset, StoreResult.generalError(updateResult.getFailureDetail()));
            }
            return new Pair<>(updatedAsset, StoreResult.success());
        }
        return new Pair<>(updatedAsset, createResult);
    }

    private StoreResult<Asset> remove(String assetId) {
        return assetIndex.deleteById(assetId);
    }
}