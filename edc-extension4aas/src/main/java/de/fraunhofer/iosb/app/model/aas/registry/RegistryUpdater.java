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

import de.fraunhofer.iosb.app.model.aas.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;

public class RegistryUpdater extends PipelineStep<Map<URL, Collection<Service>>, Collection<Pair<Asset, Asset>>> {
    private final RegistryRepository registryRepository;

    public RegistryUpdater(RegistryRepository registryRepository) {
        this.registryRepository = registryRepository;
    }

    /**
     * The input to this function is a map with keys being registry accessUrl and value being its registered services.
     *
     * @param registries Registries with the updated environments
     * @return Old and new asset of each registry for synchronizer to create changeSet
     */
    @Override
    public PipelineResult<Collection<Pair<Asset, Asset>>> apply(Map<URL, Collection<Service>> registries) {
        Collection<Pair<Asset, Asset>> result = new ArrayList<>();
        registries.forEach((registryUrl, services) -> {
            var storedEnvironments = Optional.ofNullable(registryRepository.getEnvironments(registryUrl)).orElse(List.of());
            services.forEach(service -> result.add(new Pair<>(getCorresponding(storedEnvironments, service).environment(), service.environment())));

            registryRepository.update(new Registry(registryUrl, services));
        });

        return PipelineResult.success(result);
    }

    /*
        Find service with same accessUrl in a collection of services. If not found, empty service with same URL is returned
     */
    private @Nonnull Service getCorresponding(Collection<Service> services, Service toFind) {
        return services.stream()
                .filter(toFind::equals)
                .findFirst()
                .orElse(new Service(toFind.accessUrl(), null));
    }
}
