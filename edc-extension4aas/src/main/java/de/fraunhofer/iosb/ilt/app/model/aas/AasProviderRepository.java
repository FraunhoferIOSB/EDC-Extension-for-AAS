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
package de.fraunhofer.iosb.ilt.app.model.aas;

import de.fraunhofer.iosb.ilt.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.ilt.app.model.ids.SelfDescriptionChangeListener;
import org.eclipse.edc.spi.observe.ObservableImpl;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;

public abstract class AasProviderRepository<T extends AasProvider> extends ObservableImpl<SelfDescriptionChangeListener> {
    private final Collection<T> content = new HashSet<>();

    /**
     * Returns the contents of this repository.
     *
     * @return All elements currently stored in this repository.
     */
    public Collection<T> getAll() {
        return this.content;
    }

    /**
     * Adds a new entity to the repository and notifies listeners.
     *
     * @param entity The new entity.
     * @return True if created, else false.
     */
    public boolean create(T entity) {
        if (content.add(entity)) {
            created(entity);
            return true;
        }
        return false;
    }

    /**
     * Removes entity and notifies listeners.
     *
     * @param accessUrl URL of entity to be removed
     */
    public boolean delete(URL accessUrl) {
        T entity = content.stream()
                .filter(s -> s.getAccessUrl().toString().equals(accessUrl.toString()))
                .findFirst()
                .orElse(null);

        if (entity != null) {
            removed(entity);
            return content.remove(entity);
        }
        return false;
    }


    /**
     * Update an entity. Entities are identified by their accessUrls.
     *
     * @param entity Entity to update.
     */
    public void update(T entity) {
        content.remove(entity);
        content.add(entity);
    }

    /**
     * Returns the name of the stored content type
     *
     * @return Stored content type as string
     */
    public abstract String contentType();

    protected abstract void created(T created);

    protected abstract void removed(T removed);
}
