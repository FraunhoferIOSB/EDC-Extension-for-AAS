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
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.observe.ObservableImpl;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType.REGISTRY;
import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE;

/**
 * Self-description repository, also an observable so that on removal
 * of self-description, AssetIndex / ContractStore can be synchronized
 */
public class SelfDescriptionRepository extends ObservableImpl<SelfDescriptionChangeListener> {

    private final Collection<Service> services;
    private final Collection<Registry> registries;

    public SelfDescriptionRepository() {
        super();
        services = new HashSet<>();
        registries = new HashSet<>();
    }

    public Set<SelfDescriptionMetaInformation> getAllSelfDescriptionMetaInformation() {
        var selfDescriptions = new HashSet<SelfDescriptionMetaInformation>();
        services.forEach(service -> selfDescriptions.add(new SelfDescriptionMetaInformation(service.accessUrl(), SERVICE)));
        registries.forEach(registry -> selfDescriptions.add(new SelfDescriptionMetaInformation(registry.accessUrl(), REGISTRY)));

        return selfDescriptions;
    }

    public Collection<Asset> getAllEnvironments() {
        var environments = new ArrayList<Asset>();
        environments.addAll(services.stream().map(Service::environment).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        environments.addAll(registries.stream().map(Registry::services).flatMap(Collection::stream).map(Service::environment).toList());
        return environments;
    }

    public Collection<Asset> getEnvironments(String accessUrl) {
        var environments = new ArrayList<Asset>();
        environments.addAll(services.stream().filter(service -> service.accessUrl().toString().equals(accessUrl)).map(Service::environment).collect(ArrayList::new, ArrayList::add, ArrayList::addAll));
        environments.addAll(registries.stream().filter(registry -> registry.accessUrl().toString().equals(accessUrl)).map(Registry::services).flatMap(Collection::stream).map(Service::environment).toList());
        return environments;
    }

    public @Nullable Registry getOfferingRegistry(Service service) {
        // TODO what if more than one registries offer this service / service is also registered as standalone?
        return registries.stream().filter(reg -> reg.offers(service)).findFirst().orElseThrow();
    }

    /**
     * Adds a new service to the repository with the given url.
     *
     * @param accessUrl Access URL of the new service.
     * @return True if created, else false.
     */
    public boolean createService(URL accessUrl) {
        var service = new Service(accessUrl, null);

        if (services.add(service)) {
            invokeForEach(listener -> listener.created(service));
            return true;
        }
        return false;
    }

    /**
     * Adds a new registry to the repository with the given url.
     *
     * @param accessUrl Access URL of the new registry.
     * @return True if created, else false.
     */
    public boolean createRegistry(URL accessUrl) {
        var registry = new Registry(accessUrl, null);

        if (registries.add(registry)) {
            invokeForEach(listener -> listener.created(registry));
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
     * Update a registry. Registries are identified by their accessUrls
     *
     * @param toUpdate Registry with new content
     */
    public void updateRegistry(Registry toUpdate) {
        registries.remove(toUpdate);
        registries.add(toUpdate);
    }

    /**
     * Remove service and notify listeners.
     *
     * @param accessUrl URL of service to be removed
     */
    public void removeService(URL accessUrl) {
        // Before we remove the self-description, notify listeners (remove assets/contracts from edc)
        services.stream()
                .filter(service -> service.accessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .ifPresent(service -> {
                    services.remove(service);
                    invokeForEach(listener -> listener.removed(service));
                });
    }

    /**
     * Remove registry and notify listeners.
     *
     * @param accessUrl URL of registry to be removed
     */
    public void removeRegistry(URL accessUrl) {
        // Before we remove the self-description, notify listeners (remove assets/contracts from edc)
        registries.stream()
                .filter(registry -> registry.accessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .ifPresent(registry -> {
                    registries.remove(registry);
                    invokeForEach(listener -> listener.removed(registry));
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

    public record SelfDescriptionMetaInformation(URL url, SelfDescriptionSourceType type) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SelfDescriptionMetaInformation that = (SelfDescriptionMetaInformation) o;
            return Objects.equals(url, that.url) && type == that.type;
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, type);
        }
    }
}
