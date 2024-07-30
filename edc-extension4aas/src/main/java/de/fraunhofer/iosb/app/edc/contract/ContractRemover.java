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
package de.fraunhofer.iosb.app.edc.contract;

import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;

import java.util.Collection;
import java.util.List;

/**
 * Removes contracts attached to Assets.
 */
public class ContractRemover extends PipelineStep<String, Void> {

    private final ContractDefinitionStore contractDefinitionStore;

    public ContractRemover(ContractDefinitionStore contractDefinitionStore) {
        this.contractDefinitionStore = contractDefinitionStore;
    }

    /**
     * Deletes any contract linked to a given assetId.
     *
     * @param assetId Asset ID
     */
    @Override
    public Void execute(String assetId) {
        var assetFilterExpression = new Criterion(Asset.PROPERTY_ID, "=", assetId);
        var queryAssetFilter = QuerySpec.Builder.newInstance().filter(List.of(assetFilterExpression)).build();

        var failures = contractDefinitionStore.findAll(queryAssetFilter)
                .map(contract -> contractDefinitionStore.deleteById(contract.getId()))
                .filter(AbstractResult::failed)
                .map(AbstractResult::getFailureMessages)
                .flatMap(Collection::stream)
                .toList();
        if (!failures.isEmpty()) {
            throw new EdcException("Could not delete contracts for %s. %s".formatted(assetId, failures.toArray()));
        }
        return null;
    }
}
