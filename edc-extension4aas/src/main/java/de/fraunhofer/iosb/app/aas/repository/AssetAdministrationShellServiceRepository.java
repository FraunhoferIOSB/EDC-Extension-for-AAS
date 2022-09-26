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
package de.fraunhofer.iosb.app.aas.repository;

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds all AAS services (e.g., FA³ST, BaSyx)
 */
public class AssetAdministrationShellServiceRepository<T> {
    private Map<URL, T> services;

    public AssetAdministrationShellServiceRepository() {
        services = new HashMap<>();
    }

    public T getService(URL serviceUrl) {
        return services.get(serviceUrl);
    }

    public Collection<T> getAllValues() {
        return services.values();
    }

    public Collection<URL> getAllEntries() {
        return services.keySet();
    }

    public void add(URL serviceUrl, T service) {
        services.put(serviceUrl, service);
    }

    public void remove(URL serviceUrl) {
        services.remove(serviceUrl);
    }

    public void update(URL serviceUrl, T service) {
        if (services.containsKey(serviceUrl)) {
            services.put(serviceUrl, service);
        }
    }

}
