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
package de.fraunhofer.iosb.ilt.dataspace.app.stores.repository;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Generic store for AAS server instances.
 *
 * @param <T> the type of AAS server (e.g., repository).
 */
public abstract class AasStore<T> {
    /** Underlying map holding the stored AAS servers keyed by their URI. */
    protected final Map<URI, T> store;


    /**
     * Default constructor, initializing the store with a concurrent hash map.
     */
    public AasStore() {
        this.store = new ConcurrentHashMap<>();
    }


    /**
     * Get an AAS server by its URI.
     *
     * @param uri the URI of the server.
     * @return the AAS server.
     */
    public Optional<T> get(URI uri) {
        return Optional.ofNullable(store.get(uri));
    }


    /**
     * Returns all stored AAS servers.
     *
     * @return all stored AAS servers.
     */
    public List<T> getAll() {
        return store.values().stream().toList();
    }


    /**
     * Store a new AAS server or update an existing one.
     *
     * @param uri the URI of the AAS server.
     * @param t the AAS server.
     * @return true if no AAS server was stored for this URI, else false.
     */
    public boolean put(URI uri, T t) {
        return null == store.put(uri, t);
    }


    /**
     * Remove an AAS store by its URI.
     *
     * @param uri the URI of the AAS server.
     * @return the removed AAS server.
     */
    public T remove(URI uri) {
        return store.remove(uri);
    }
}
