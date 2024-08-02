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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.observe.ObservableImpl;
import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Self-description repository, also an observable so that on removal
 * of self-description, AssetIndex / ContractStore can be synchronized
 */
public class SelfDescriptionRepository extends ObservableImpl<SelfDescriptionChangeListener> {

    private final Map<SelfDescriptionMetaInformation, Asset> content;

    public SelfDescriptionRepository() {
        super();
        content = new HashMap<>();
    }

    public Set<SelfDescriptionMetaInformation> getAllSelfDescriptionMetaInformation() {
        return content.keySet();
    }

    public Map<SelfDescriptionMetaInformation, Asset> getAllSelfDescriptions() {
        return content;
    }

    /**
     * Return self-description associated with this URL
     *
     * @param aasUrl URL determining self description to be returned
     * @return self-description asset associated with AAS URL or empty asset
     */
    public @Nullable Asset getSelfDescriptionAsset(String aasUrl) {
        return content.get(findByUrl(aasUrl));
    }

    /**
     * Create entry for a self-description.
     * This will indirectly call the synchronizer to fetch the AAS
     * service's contents and build the self-description from it.
     *
     * @param url  URL of self-description to be created
     * @param type Type of self-description to be created
     */
    public void createSelfDescription(URL url, SelfDescriptionSourceType type) {
        var metaInformation = new SelfDescriptionMetaInformation(url, type);

        content.put(metaInformation, null);
        this.getListeners().forEach(listener -> listener.created(metaInformation));
    }

    /**
     * Update self description.
     *
     * @param aasUrl         URL of self-description to be updated
     * @param newEnvironment updated environment from which self-description is
     *                       created
     */
    public void updateSelfDescription(String aasUrl, Asset newEnvironment) {
        content.put(findByUrl(aasUrl), newEnvironment);
    }

    private SelfDescriptionMetaInformation findByUrl(String url) {
        return content.keySet().stream()
                .filter(entry -> entry.url().toString().equals(url))
                .findFirst()
                .orElse(null);
    }

    /**
     * Remove self-description and notify listeners.
     *
     * @param url URL of self-description to be updated
     */
    public void removeSelfDescription(String url) {
        var metaInformation = findByUrl(url);
        // Before we remove the self-description, notify listeners (synchronizer -> remove assets from edc)
        this.getListeners().forEach(listener -> listener.removed(metaInformation));
        content.remove(metaInformation);
    }

    public enum SelfDescriptionSourceType { SERVICE, REGISTRY }

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
