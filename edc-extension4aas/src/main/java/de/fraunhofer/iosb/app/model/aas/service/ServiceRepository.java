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

import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.observe.ObservableImpl;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * Self-description repository, also an observable so that on removal
 * of self-description, AssetIndex / ContractStore can be synchronized
 */
public class ServiceRepository extends ObservableImpl<SelfDescriptionChangeListener> {

    private final Collection<Service> services;

    public ServiceRepository() {
        super();
        services = new HashSet<>();
    }

    public Collection<Service> getAll() {
        return services;
    }

    /**
     * Returns the environments offered by all stored AAS services.
     * If a stored AAS service has no environment, the element is filtered out.
     *
     * @return All environments currently stored for all registered AAS services.
     */
    public Collection<Asset> getAllEnvironments() {
        return services.stream()
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
        return services.stream()
                .filter(service ->
                        service.getAccessUrl().toString()
                                .equals(serviceUrl.toString()))
                .findAny()
                .orElseThrow(() ->
                        new IllegalArgumentException("AAS service with URL %s not found".formatted(serviceUrl)))
                .environment();
    }

    /**
     * Adds a new service to the repository with the given url.
     *
     * @param accessUrl Access URL of the new service.
     * @return True if created, else false.
     */
    public boolean create(URL accessUrl) {
        var service = new Service(accessUrl);

        if (services.add(service)) {
            invokeForEach(listener -> listener.created(service));
            return true;
        }
        return false;
    }

    /**
     * Update a service. Services are identified by their accessUrls.
     *
     * @param service Service to update.
     */
    public void updateService(Service service) {
        services.remove(service);
        services.add(service);
    }

    /**
     * Remove service and notify listeners.
     *
     * @param accessUrl URL of service to be removed
     */
    public boolean delete(URL accessUrl) {
        // Before we remove the service, notify listeners (remove assets/contracts from edc)
        Service service = services.stream()
                .filter(s -> s.getAccessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .orElse(null);

        if (service != null) {
            invokeForEach(listener -> listener.removed(service));
            return services.remove(service);
        }
        return false;
    }

    public enum SelfDescriptionSourceType {
        /**
         * An AAS service such as FA³ST
         */
        SERVICE,
        /**
         * An AAS registry as specified in AAS documents
         */
        REGISTRY
    }

}
