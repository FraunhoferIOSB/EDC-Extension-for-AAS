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
import de.fraunhofer.iosb.app.model.aas.Registry;
import de.fraunhofer.iosb.app.model.aas.Service;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Objects;


/**
 * Holds a pipeline for removing assets and contracts.
 */
public class CleanUpService implements SelfDescriptionChangeListener {

    private final Pipeline<Asset, Void> servicePipeline;

    CleanUpService(Pipeline<Asset, Void> servicePipeline) {
        this.servicePipeline = servicePipeline;
    }

    @Override
    public void removed(Service service) {
        servicePipeline.execute(service.environment());
    }

    @Override
    public void removed(Registry registry) {
        registry.services().stream()
                .map(Service::environment)
                .forEach(servicePipeline::execute);
    }

    public static class Builder {
        private AssetIndex assetIndex;
        private Monitor monitor;
        private ContractDefinitionStore contractDefinitionStore;
        private PolicyDefinitionStore policyDefinitionStore;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder assetIndex(AssetIndex assetIndex) {
            this.assetIndex = assetIndex;
            return this;
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder contractDefinitionStore(ContractDefinitionStore contractDefinitionStore) {
            this.contractDefinitionStore = contractDefinitionStore;
            return this;
        }

        public Builder policyDefinitionStore(PolicyDefinitionStore policyDefinitionStore) {
            this.policyDefinitionStore = policyDefinitionStore;
            return this;
        }

        public CleanUpService build() {
            Objects.requireNonNull(assetIndex);
            Objects.requireNonNull(contractDefinitionStore);
            Objects.requireNonNull(monitor);
            Objects.requireNonNull(policyDefinitionStore);

            var pipeline = new Pipeline.Builder<Asset, ChangeSet<Asset, String>>()
                    .initialStep(PipelineStep.create(asset -> new ChangeSet.Builder<Asset, String>()
                            .remove(AssetUtil
                                    .flatMapAssets(asset).stream()
                                    .map(Asset::getId)
                                    .toList())
                            .build()))
                    .step(new AssetRegistrar(assetIndex, monitor))
                    .step(new ContractRegistrar(contractDefinitionStore, policyDefinitionStore, monitor))
                    .monitor(monitor)
                    .build();

            return new CleanUpService(pipeline);
        }
    }
}
