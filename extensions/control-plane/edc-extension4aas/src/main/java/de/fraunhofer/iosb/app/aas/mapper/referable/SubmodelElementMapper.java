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
package de.fraunhofer.iosb.app.aas.mapper.referable;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.client.AasServerClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Map any instance of SubmodelElement to an EDC asset.
 */
public class SubmodelElementMapper extends ReferableMapper {

    public static final String SMC_CHILDREN_LOCATION = "value";


    public SubmodelElementMapper(AasServerClient client) {
        super(client);
    }


    /**
     * Map the submodelElement to an EDC asset. All metadata of the submodelElement is serialized into a map which is subsequently added as Asset.properties. The DataAddress points
     * to the submodelElement from the AAS server using
     *
     * @param parent Parent element to resolve the path in the environment.
     * @param submodelElement SubmodelElement to map.
     * @param <E> Concrete type of the submodelElement.
     * @return Mapped submodelElement.
     */
    public <E extends SubmodelElement> Asset map(Reference parent, E submodelElement) {
        return map(parent, submodelElement, false);
    }


    /* May contain traces of recursion */
    private <E extends SubmodelElement> Asset map(Reference parent, E submodelElement, boolean isListElement) {
        Reference reference = AasUtils.toReference(parent, submodelElement);

        if (isListElement) {
            submodelElement.setIdShort(null);
        }

        var dataAddress = createDataAddress(reference);

        Asset.Builder assetBuilder = super.map(submodelElement)
                .id(generateId(reference));

        if (Configuration.getInstance().useAasDataPlane()) {
            assetBuilder.dataAddress(dataAddress);
        }
        else {
            assetBuilder.dataAddress(dataAddress.asHttpDataAddress());
        }

        Optional<List<Asset>> childrenMaybe = handleChildren(reference, submodelElement);

        childrenMaybe.ifPresent(assets -> assetBuilder.property(SMC_CHILDREN_LOCATION, assets));

        return assetBuilder.build();
    }


    private <T extends SubmodelElement> Optional<List<Asset>> handleChildren(Reference parent, T submodelElement) {
        Optional<List<SubmodelElement>> childrenMaybe = getChildElements(submodelElement);

        List<Asset> mappedChildren = null;

        if (childrenMaybe.isPresent() && !childrenMaybe.get().isEmpty()) {
            List<SubmodelElement> children = childrenMaybe.get();

            mappedChildren = new ArrayList<>(children.size());

            for (int i = 0; i < children.size(); i++) {
                SubmodelElement child = children.get(i);

                // AASd-120
                if (child.getIdShort() == null && submodelElement instanceof SubmodelElementList) {
                    child.setIdShort(String.valueOf(i));
                    mappedChildren.add(map(parent, child, true));
                }
                else {
                    mappedChildren.add(map(parent, child, false));
                }
            }
        }

        return Optional.ofNullable(mappedChildren);
    }


    private <T extends SubmodelElement> Optional<List<SubmodelElement>> getChildElements(T submodelElement) {
        if (submodelElement instanceof SubmodelElementCollection collection) {
            return Optional.ofNullable(collection.getValue());
        }
        else if (submodelElement instanceof SubmodelElementList list) {
            return Optional.ofNullable(list.getValue());
        }
        // Can't have any child elements...
        return Optional.empty();
    }
}
