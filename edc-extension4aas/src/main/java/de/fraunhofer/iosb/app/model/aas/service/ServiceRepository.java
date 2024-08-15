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

    public Collection<URL> getAllServiceAccessUrls() {
        return services.stream().map(Service::accessUrl).toList();
    }

    public Collection<Asset> getAllEnvironments() {
        return services.stream().map(Service::environment).toList();
    }

    public @Nullable Asset getEnvironment(URL serviceUrl) {
        return services.stream()
                .filter(service ->
                        service.accessUrl().toString()
                                .equals(serviceUrl.toString()))
                .map(Service::environment)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a new service to the repository with the given url.
     *
     * @param accessUrl Access URL of the new service.
     * @return True if created, else false.
     */
    public boolean create(URL accessUrl) {
        var service = new Service(accessUrl, null);

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
    public void delete(URL accessUrl) {
        // Before we remove the self-description, notify listeners (remove assets/contracts from edc)
        services.stream()
                .filter(service -> service.accessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .ifPresent(service -> {
                    services.remove(service);
                    invokeForEach(listener -> listener.removed(service));
                });
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
