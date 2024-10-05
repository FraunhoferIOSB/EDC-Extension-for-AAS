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

import de.fraunhofer.iosb.app.model.aas.AasProviderRepository;
import de.fraunhofer.iosb.app.model.aas.service.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Predicate;

public class RegistryRepository extends AasProviderRepository<Registry> {

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
    public @Nullable Collection<Service> getEnvironments(@Nonnull URL registryUrl) {
        return getEnvironments(registry -> registry.getAccessUrl().toString().equals(registryUrl.toString()));
    }

    @Override
    public String contentType() {
        return Registry.class.getSimpleName();
    }

    @Override
    protected void created(Registry created) {
        invokeForEach(listener -> listener.created(created));
    }

    @Override
    protected void removed(Registry removed) {
        invokeForEach(listener -> listener.removed(removed));
    }

    private Collection<Service> getEnvironments(Predicate<Registry> registryPredicate) {
        return getAll().stream()
                .filter(registryPredicate)
                .map(Registry::services)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
    }
}