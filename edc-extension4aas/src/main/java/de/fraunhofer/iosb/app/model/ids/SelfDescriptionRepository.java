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

import org.eclipse.dataspaceconnector.spi.EdcException;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository holding selfDescriptions. The ID of a self description is the
 * URL of the AAS instance holding the AAS model that is represented by
 * the self description.
 */
public class SelfDescriptionRepository {

    private Map<URL, SelfDescription> selfDescriptions;

    public SelfDescriptionRepository() {
        selfDescriptions = new HashMap<>();
    }

    /**
     * Get a self description by its underlying AAS service's URL.

     * @param aasServiceUrl The underlying AAS service URL of the self description
     * @return The matching self description, or null if this repository contains no
     *         mapping for the URL
     */
    public SelfDescription get(URL aasServiceUrl) {
        return selfDescriptions.get(aasServiceUrl);
    }

    /**
     * Get all entries of this repository
     */
    public Map<URL, SelfDescription> getAll() {
        return selfDescriptions;
    }
    
    /**
     * Add a new self description to the repository

     * @param aasServiceUrl   URL of the underlying AAS service of this self
     *                        description's AAS model.
     * @param selfDescription The self description to be added
     */
    public void add(URL aasServiceUrl, SelfDescription selfDescription) {
        selfDescriptions.put(aasServiceUrl, selfDescription);
    }

    /**
     * Update an existing self description

     * @param aasServiceUrl   url of the self description to be updated
     * @param selfDescription new self description
     */
    public void update(URL aasServiceUrl, SelfDescription selfDescription) {
        if (selfDescriptions.containsKey(aasServiceUrl)) {
            selfDescriptions.put(aasServiceUrl, selfDescription);
        } else {
            throw new EdcException("Self description with id " + aasServiceUrl + "does not exist");
        }
    }

    /**
     * Remove a self description by its underlying AAS service's URL

     * @param aasServiceUrl The underlying URL
     */
    public SelfDescription remove(URL aasServiceUrl) {
        return selfDescriptions.remove(aasServiceUrl);
    }
}
