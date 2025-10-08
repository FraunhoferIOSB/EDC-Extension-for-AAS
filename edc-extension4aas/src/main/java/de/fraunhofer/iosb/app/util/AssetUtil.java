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

import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;

import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.CONCEPT_DESCRIPTIONS_LOCATION;
import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.SHELLS_LOCATION;
import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.SUBMODELS_LOCATION;
import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.identifiable.SubmodelMapper.SUBMODEL_ELEMENT_LOCATION;

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
        var assets = getChildren(environmentAsset, SHELLS_LOCATION);
        assets.addAll(getChildren(environmentAsset, CONCEPT_DESCRIPTIONS_LOCATION));

        var submodels = getChildren(environmentAsset, SUBMODELS_LOCATION);
        assets.addAll(submodels);

        // Recursively flatten submodel element structure
        assets.addAll(submodels.stream()
                .map(submodel -> getChildren(submodel, SUBMODEL_ELEMENT_LOCATION))
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
        if (childrenMaybe instanceof List<?> childrenMaybeList &&
                !childrenMaybeList.isEmpty() &&
                childrenMaybeList.get(0) instanceof Asset) {
            return new ArrayList<>((List<Asset>) childrenMaybe);
        }
        return new ArrayList<>();
    }

    public static void forEachSubmodelElementAssetRec(Asset parent, UnaryOperator<Asset> function) {
        getChildrenRec(parent).forEach(function::apply);
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
