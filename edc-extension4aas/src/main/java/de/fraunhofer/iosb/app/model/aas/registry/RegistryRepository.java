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

import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import org.eclipse.edc.spi.observe.ObservableImpl;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class RegistryRepository extends ObservableImpl<SelfDescriptionChangeListener> {
    private final Collection<Registry> registries;

    public RegistryRepository() {
        super();
        this.registries = new HashSet<>();
    }

    /**
     * Adds a new registry to the repository with the given url.
     *
     * @param accessUrl Access URL of the new registry.
     * @return True if created, else false.
     */
    public boolean create(URL accessUrl) {
        var registry = new Registry(accessUrl);

        if (registries.add(registry)) {
            invokeForEach(listener -> listener.created(registry));
            return true;
        }
        return false;
    }

    /**
     * Returns all URLs of the stored registries
     *
     * @return URLs of all registries currently stored.
     */
    public Collection<Registry> getAll() {
        return registries;
    }

    /**
     * Returns all stored environments of all registries
     *
     * @return All stored environment.
     */
    public Collection<Service> getAllEnvironments() {
        return getEnvironments(registry -> true);
    }

    /**
     * Returns the environment of a specific registry by its URL.
     *
     * @param registryUrl The URL of the registry
     * @return The environments of this registry or null
     */
    public @Nullable Collection<Service> getEnvironments(URL registryUrl) {
        return getEnvironments(registry ->
                registry.getAccessUrl().toString()
                        .equals(registryUrl.toString()));
    }

    /**
     * Update a registry. Registries are identified by their accessUrls
     *
     * @param toUpdate Registry with new content
     */
    public void update(Registry toUpdate) {
        registries.remove(toUpdate);
        registries.add(toUpdate);
    }

    /**
     * Remove registry and notify listeners.
     *
     * @param accessUrl URL of registry to be removed
     */
    public boolean delete(URL accessUrl) {
        // Before we remove the registry, notify listeners (remove assets/contracts from edc)
        Registry registry = registries.stream()
                .filter(r -> r.getAccessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .orElse(null);

        if (registry != null) {
            invokeForEach(listener -> listener.removed(registry));
            return registries.remove(registry);
        }

        return false;
    }

    private Collection<Service> getEnvironments(Predicate<Registry> registryPredicate) {
        return registries.stream()
                .filter(registryPredicate)
                .map(Registry::services)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }
}