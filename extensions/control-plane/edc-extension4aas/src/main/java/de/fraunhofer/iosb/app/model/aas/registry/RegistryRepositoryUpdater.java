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
package de.fraunhofer.iosb.app.model.aas.registry;

import de.fraunhofer.iosb.aas.lib.model.impl.Registry;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Update a registry
 */
public class RegistryRepositoryUpdater extends PipelineStep<Collection<Registry>, Collection<Pair<Asset, Asset>>> {
    private final RegistryRepository registryRepository;

    public RegistryRepositoryUpdater(RegistryRepository registryRepository) {
        this.registryRepository = registryRepository;
    }

    /**
     * The input to this function is a map with keys being registry accessUrl and value being its registered services.
     *
     * @param registries Registries with the updated environments
     * @return Old and new asset of each registry for synchronizer to create changeSet
     */
    @Override
    public PipelineResult<Collection<Pair<Asset, Asset>>> apply(Collection<Registry> registries) {
        Collection<Pair<Asset, Asset>> result = new ArrayList<>();
        registries.forEach(registry -> {
            var storedEnvironments =
                    Optional.ofNullable(registryRepository.getEnvironments(registry.baseUri())).orElse(List.of());

            Optional.ofNullable(registry.services())
                    .orElse(List.of())
                    .forEach(service ->
                            result.add(new Pair<>(
                                    getCorresponding(storedEnvironments, service),
                                    service.getEnvironment())));

            registryRepository.update(registry);
        });

        return PipelineResult.success(result);
    }

    /*
        Find environment of service with same accessUrl in a collection of services. If not found, return null
     */
    private Asset getCorresponding(Collection<Service> services, Service toFind) {
        return services.stream()
                .filter(toFind::equals)
                .findFirst()
                .map(Service::getEnvironment)
                .orElse(null);
    }
}
