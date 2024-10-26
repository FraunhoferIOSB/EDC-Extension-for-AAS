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
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.app.pipeline.Pipeline;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.AssetUtil;
import de.fraunhofer.iosb.model.aas.registry.Registry;
import de.fraunhofer.iosb.model.aas.service.Service;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Listens for removals of registries and services from the EDC.
 * If a registry or service is removed from the EDC, the CleanUpService will remove any EDC assets and contracts
 * associated with it.
 */
public class CleanUpService implements SelfDescriptionChangeListener {

    private final Pipeline<Asset, Void> servicePipeline;

    private CleanUpService(Pipeline<Asset, Void> servicePipeline) {
        this.servicePipeline = servicePipeline;
    }

    @Override
    public void removed(Service service) {
        if (service.environment() != null) {
            servicePipeline.execute(service.environment());
        }
    }

    @Override
    public void removed(Registry registry) {
        Optional.ofNullable(registry.services())
                .orElse(List.of()).stream()
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

        /**
         * Removal of assets after unregistering a service / registry.
         */
        public Builder assetIndex(AssetIndex assetIndex) {
            this.assetIndex = assetIndex;
            return this;
        }

        /**
         * Logging capabilities for the pipeline
         */
        public Builder monitor(Monitor monitor) {
            this.monitor = monitor.withPrefix("CleanUpService");
            return this;
        }

        /**
         * Removal of contracts after unregistering a service / registry.
         */
        public Builder contractDefinitionStore(ContractDefinitionStore contractDefinitionStore) {
            this.contractDefinitionStore = contractDefinitionStore;
            return this;
        }

        /**
         * Removal of loose policies after unregistering a service / registry.
         */
        public Builder policyDefinitionStore(PolicyDefinitionStore policyDefinitionStore) {
            this.policyDefinitionStore = policyDefinitionStore;
            return this;
        }

        public CleanUpService build() {
            Objects.requireNonNull(monitor);

            var pipeline = new Pipeline.Builder<Asset, ChangeSet<Asset, String>>()
                    .initialStep(PipelineStep.create(asset -> new ChangeSet.Builder<Asset, String>()
                            .remove(AssetUtil
                                    .flatMapAssets(asset).stream()
                                    .map(Asset::getId)
                                    .toList())
                            .build()))
                    .step(PipelineStep.create(input -> {
                        monitor.debug("Started pipeline for %s assets.".formatted(input.toRemove().size()));
                        return input;
                    }))
                    .step(new AssetRegistrar(Objects.requireNonNull(assetIndex), monitor))
                    .step(new ContractRegistrar(
                            Objects.requireNonNull(contractDefinitionStore),
                            Objects.requireNonNull(policyDefinitionStore),
                            monitor))
                    .monitor(monitor)
                    .build();

            return new CleanUpService(pipeline);
        }
    }
}
