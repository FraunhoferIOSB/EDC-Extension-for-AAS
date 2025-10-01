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

import de.fraunhofer.iosb.app.edc.asset.AssetRegistrar;
import de.fraunhofer.iosb.app.edc.contract.ContractRegistrar;
import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResourceRegistrar extends PipelineStep<ChangeSet<Asset, String>, Void> {

    private final AssetRegistrar assetRegistrar;
    private final ContractRegistrar contractRegistrar;
    private final Monitor monitor;

    public ResourceRegistrar(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore,
                             PolicyDefinitionStore policyDefinitionStore, Monitor monitor) {
        this.monitor = monitor;

        this.assetRegistrar = new AssetRegistrar(assetIndex);
        this.contractRegistrar = new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor);
    }

    /**
     * Adds/Removes assets given the change set.
     *
     * @param changeSet Assets to add/remove.
     * @return Asset IDs of all the added/removed assets
     */
    @Override
    public PipelineResult<Void> apply(ChangeSet<Asset, String> changeSet) {

        var addAssetsResults = changeSet.toAdd().stream().map(assetRegistrar::createAsset).toList();
        var removeAssetsResults = changeSet.toRemove().stream().map(assetRegistrar::removeAsset).toList();

        var updateContractResult = contractRegistrar.updateContract(
                changeSet.toAdd().stream().map(Asset::getId).toList(),
                changeSet.toRemove().stream().toList());

        if (addAssetsResults.stream().anyMatch(AbstractResult::failed) || removeAssetsResults.stream().anyMatch(AbstractResult::failed) ||
                updateContractResult.failed()) {
            var resultStreams = Stream.concat(
                    addAssetsResults.stream()
                            .map(AbstractResult::getFailureMessages)
                            .flatMap(List::stream),
                    removeAssetsResults.stream()
                            .map(AbstractResult::getFailureMessages)
                            .flatMap(List::stream));

            var resultMessages = resultStreams.collect(Collectors.toCollection(ArrayList::new));
            resultMessages.addAll(updateContractResult.getFailureMessages());

            return PipelineResult.failure(PipelineFailure.warning(resultMessages));

        }

        var added = addAssetsResults.stream().filter(AbstractResult::succeeded).count();
        var removed = removeAssetsResults.stream().filter(AbstractResult::succeeded).count();

        if (added + removed > 0) {
            monitor.info("Added %s, removed %s assets".formatted(added, removed));
        }

        return PipelineResult.success(null);
    }

}
