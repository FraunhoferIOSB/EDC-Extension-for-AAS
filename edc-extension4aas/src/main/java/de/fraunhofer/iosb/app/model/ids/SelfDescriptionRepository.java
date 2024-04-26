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

import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import org.eclipse.edc.spi.observe.ObservableImpl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Self-description repository, also an observable so that on removal
 * of self-description, AssetIndex / ContractStore can be synchronized
 */
public class SelfDescriptionRepository extends ObservableImpl<SelfDescriptionChangeListener> {

    private final Map<URL, SelfDescription> content;

    public SelfDescriptionRepository() {
        content = new HashMap<>();
    }

    public Set<Entry<URL, SelfDescription>> getAllSelfDescriptions() {
        return content.entrySet();
    }

    /**
     * Return self-description associated with this URL
     *
     * @param aasUrl URL determining self description to be returned
     * @return self-description associated with AAS URL
     */
    public SelfDescription getSelfDescription(URL aasUrl) {
        return content.get(aasUrl);
    }

    /**
     * Create entry for a self-description.
     * This will indirectly call the synchronizer to fetch the AAS
     * service's contents and build the self-description from it.
     *
     * @param aasUrl The URL of the AAS service.
     */
    public void createSelfDescription(URL aasUrl) {
        content.put(aasUrl, null);
        this.getListeners().forEach(listener -> listener.created(aasUrl));
    }

    /**
     * Update self description.
     *
     * @param aasUrl         URL of self-description to be updated
     * @param newEnvironment updated environment from which self-description is
     *                       created
     */
    public void updateSelfDescription(URL aasUrl, CustomAssetAdministrationShellEnvironment newEnvironment) {
        content.put(aasUrl, new SelfDescription(newEnvironment));
    }

    /**
     * Remove self-description and notify listeners.
     *
     * @param aasUrl URL of self-description to be updated
     */
    public void removeSelfDescription(URL aasUrl) {
        var toBeRemoved = content.remove(aasUrl);
        this.getListeners().forEach(listener -> listener.removed(toBeRemoved));
    }
}
