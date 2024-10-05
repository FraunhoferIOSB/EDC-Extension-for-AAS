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
package de.fraunhofer.iosb.app.model.aas.service;

import de.fraunhofer.iosb.app.model.aas.AasProviderRepository;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;

/**
 * Self-description repository, also an observable so that on removal
 * of self-description, AssetIndex / ContractStore can be synchronized
 */
public class ServiceRepository extends AasProviderRepository<Service> {

    public Collection<Asset> getAllEnvironments() {
        return getAll().stream()
                .map(Service::environment)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Returns the environment offered by the AAS service with the given URL.
     * If the stored AAS service has no environment, null is returned.
     * If no AAS service is stored for the given serviceUrl, an IllegalArgumentException is thrown.
     *
     * @param serviceUrl The service URL for which the environment is to be returned.
     * @return The AAS environment in form of an EDC Asset.
     */
    public @Nullable Asset getEnvironment(URL serviceUrl) {
        return getAll().stream()
                .filter(service ->
                        service.getAccessUrl().toString()
                                .equals(serviceUrl.toString()))
                .findAny()
                .orElseThrow(() ->
                        new IllegalArgumentException("AAS service with URL %s not found".formatted(serviceUrl)))
                .environment();
    }

    @Override
    public String contentType() {
        return Service.class.getSimpleName();
    }

    @Override
    protected void created(Service created) {
        invokeForEach(listener -> listener.created(created));
    }

    @Override
    protected void removed(Service removed) {
        invokeForEach(listener -> listener.removed(removed));
    }
}
