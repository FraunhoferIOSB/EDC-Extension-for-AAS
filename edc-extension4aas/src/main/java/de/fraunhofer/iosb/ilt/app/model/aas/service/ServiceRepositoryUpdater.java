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
package de.fraunhofer.iosb.ilt.app.model.aas.service;

import de.fraunhofer.iosb.ilt.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.ilt.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.ilt.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.ilt.app.util.Pair;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

/**
 * TODO correct package?
 */
public class ServiceRepositoryUpdater extends PipelineStep<Service, Pair<Asset, Asset>> {

    private final ServiceRepository services;

    public ServiceRepositoryUpdater(ServiceRepository serviceRepository) {
        this.services = serviceRepository;
    }

    /**
     * For each service incoming: First, check if the service is offered by a registry.
     * If it is, then the updated environment asset is stored within the registry.
     * Else, store as standalone service.
     *
     * @param service URL and updated assets of service
     * @return Old and new asset for synchronizer to create changeSet
     */
    @Override
    public PipelineResult<Pair<Asset, Asset>> apply(Service service) {
        var old = services.getEnvironment(service.getAccessUrl());

        if (old != null) {
            // Create deep copy of old asset
            old = old.toBuilder().build();
        }
        services.update(service);

        return PipelineResult.success(new Pair<>(old, service.environment()));
    }

}
