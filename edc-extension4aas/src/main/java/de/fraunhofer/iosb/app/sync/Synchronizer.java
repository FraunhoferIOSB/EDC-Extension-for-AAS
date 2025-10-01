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

import de.fraunhofer.iosb.app.AasExtension;
import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;


/**
 * A synchronizer gets as input two assets:
 * - The asset holding the currently stored assets for an AAS service
 * and a more current asset built from the AAS environment.
 * The synchronizer now checks which assets got added / removed
 * since the last synchronization and passes them on to the next pipeline step as a ChangeSet
 * consisting of the new assets and the IDs of the assets to be removed.
 */
public class Synchronizer extends PipelineStep<Collection<Pair<Asset, Asset>>, ChangeSet<Asset, String>> {

    private final AssetIndex assetIndex;
    private final ContractDefinitionStore contractDefinitionStore;

    public Synchronizer(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore, PolicyDefinitionStore policyDefinitionStore) {
        this.assetIndex = assetIndex;
        this.contractDefinitionStore = contractDefinitionStore;
    }

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

        Collection<String> toRemove = new ArrayList<>();
        Collection<Asset> toAdd = new ArrayList<>();

        for (var entry : oldAndNewAssets) {
            // New environment cannot be null.
            Objects.requireNonNull(entry.second());
            var oldEnvironment = entry.first() != null ? AssetUtil.flatMapAssets(entry.first()) :
                    new ArrayList<Asset>();

            var newEnvironment = AssetUtil.flatMapAssets(entry.second());

            var newAssetIds = newEnvironment.stream().map(Asset::getId).toList();

            var alreadyStoredAssetsQuerySpec = QuerySpec.Builder.newInstance()
                    .filter(Criterion.criterion(Asset.PROPERTY_ID, "in", newAssetIds))
                    .build();

            List<Asset> alreadyStoredAssets = assetIndex.queryAssets(alreadyStoredAssetsQuerySpec).toList();

            toRemove.addAll(oldEnvironment.stream().filter(oldElement -> absent(newEnvironment, oldElement)).map(Asset::getId).toList());
            toAdd.addAll(newEnvironment.stream()
                    .filter(newElement -> absent(oldEnvironment, newElement))
                    // If AssetIndex cannot find element, we need to add it again.
                    .filter(asset -> absent(alreadyStoredAssets, asset))
                    .toList());

            var contract =
                    contractDefinitionStore.findAll(QuerySpec.max())
                            .filter(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator") != null)
                            .filter(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator").equals(AasExtension.ID))
                            .findFirst().orElse(null);

            if (contract != null) {
                var assetsSelector =
                        contract.getAssetsSelector().stream()
                                .map(Criterion::getOperandRight)
                                .map(x -> (List<String>) x)
                                .map(x -> x.get(0))
                                .toList();

                // Re-add all Assets that are not registered to the default contract.
                toAdd.addAll(newEnvironment.stream()
                        .filter(asset -> assetsSelector.stream()
                                .noneMatch(contractRegisteredAssetId -> asset.getId().equals(contractRegisteredAssetId)))
                        .toList());

                // Re-remove all Assets that are still registered to the default contract
                toRemove.addAll(assetsSelector.stream()
                        .filter(registeredAsset ->
                                newEnvironment.stream()
                                        .map(Asset::getId)
                                        .noneMatch(registeredAsset::equals)
                        )
                        .toList());
            }

        }

        toAdd = toAdd.stream().distinct().toList();
        toRemove = toRemove.stream().distinct().toList();

        return PipelineResult.success(new ChangeSet.Builder<Asset, String>().add(toAdd).remove(toRemove).build());
    }

    private boolean absent(Collection<Asset> assets, Asset asset) {
        return assets.stream().noneMatch(a -> a.getId().equals(asset.getId()));
    }
}
