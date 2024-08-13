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
package de.fraunhofer.iosb.app.model.ids;

import de.fraunhofer.iosb.app.model.aas.Registry;
import de.fraunhofer.iosb.app.model.aas.Service;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO correct package?
 */
public class SelfDescriptionUpdater extends PipelineStep<Collection<Service>, Map<Asset, Asset>> {

    private final SelfDescriptionRepository selfDescriptionRepository;

    public SelfDescriptionUpdater(SelfDescriptionRepository selfDescriptionRepository) {
        this.selfDescriptionRepository = selfDescriptionRepository;
    }

    /**
     * For each service incoming: First, check if the service is offered by a registry.
     * If it is, then the updated environment asset is stored within the registry.
     * Else, store as standalone service.
     *
     * @param services URLs and updated assets of all services
     * @return ID of old asset and new asset for synchronizer to create changeSet
     */
    @Override
    public PipelineResult<Map<Asset, Asset>> apply(Collection<Service> services) {
        Map<Asset, Asset> result = new HashMap<>();

        // Check for registries
        for (Service service : services) {
            var registry = selfDescriptionRepository.getOfferingRegistry(service);
            if (registry != null) {
                var newServices = registry.services();
                // Update the offered service.
                if (newServices.remove(service) || newServices.add(service)) {
                    selfDescriptionRepository.updateRegistry(new Registry(registry.accessUrl(), newServices));
                } else {
                    return PipelineResult.failure(
                            PipelineFailure.fatal(
                                    List.of("Could not update a registry with url %s"
                                            .formatted(registry.accessUrl()))));
                }
            }
        }

        services.forEach(selfDescriptionRepository::updateService);

        return PipelineResult.success(result);
    }
}
