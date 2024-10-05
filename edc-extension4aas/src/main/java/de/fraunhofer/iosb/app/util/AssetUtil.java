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
package de.fraunhofer.iosb.app.util;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AssetUtil {

    private AssetUtil() {
        throw new RuntimeException("Utility class");
    }

    /**
     * Flatten nested asset into collection of all containing assets.
     * The top asset will not be returned within this list.
     * Child element of the environmentAsset are stored as properties
     * of the asset under the names "shells", "conceptDescriptions" and "submodelElements".
     * Deeper nested elements (within a submodel) are stored
     * in the submodel's properties under "submodelElements".
     *
     * @param environmentAsset Representing the environment
     * @return Flattened collection of all assets
     */
    public static Collection<Asset> flatMapAssets(@Nonnull Asset environmentAsset) {
        var assets = getChildren(environmentAsset, "shells");
        assets.addAll(getChildren(environmentAsset, "conceptDescriptions"));

        var submodels = getChildren(environmentAsset, "submodels");
        assets.addAll(submodels);

        // Recursively flatten submodel element structure
        assets.addAll(submodels.stream()
                .map(submodel -> getChildren(submodel, "submodelElements"))
                .peek(assets::addAll)
                .flatMap(Collection::stream)
                .map(AssetUtil::getChildrenRec)
                .flatMap(Collection::stream)
                .toList());

        return assets;
    }

    @SuppressWarnings("unchecked") // I checked
    public static Collection<Asset> getChildren(Asset parent, String childPropertyName) {
        var childrenMaybe = parent.getProperty(childPropertyName);
        if (childrenMaybe instanceof List && !((List<?>) childrenMaybe).isEmpty() && ((List<?>) childrenMaybe).get(0) instanceof Asset) {
            return new ArrayList<>((List<Asset>) childrenMaybe);
        }
        return new ArrayList<>();
    }

    private static Collection<Asset> getChildrenRec(Asset parent) {
        var children = getChildren(parent, "value");
        if (!children.isEmpty()) {
            var grandChildren = children.stream().map(AssetUtil::getChildrenRec).flatMap(Collection::stream).toList();
            children.addAll(grandChildren);
        }
        return children;
    }
}
