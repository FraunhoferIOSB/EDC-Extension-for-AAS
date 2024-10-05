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
package de.fraunhofer.iosb.app.aas.mapper;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Not an ElementMapper since we have a different method signature.
 */
class SubmodelElementMapper {

    private final ElementMapper elementMapper;

    public SubmodelElementMapper() {
        this.elementMapper = new ElementMapper();
    }

    /* May contain traces of recursion */
    <E extends SubmodelElement> Asset map(Reference parent, E submodelElement, String accessUrl) {
        var reference = elementMapper.createReference(submodelElement.getIdShort(), parent);

        var children = getContainerElements(submodelElement).stream()
                .map(elem -> map(reference, elem, accessUrl))
                .toList();

        var dataAddress = AasDataAddress.Builder.newInstance()
                .baseUrl(accessUrl)
                .referenceChain(reference)
                .build();

        // Display the modeling type of the element, i.e. "Property"/"Operation"/...
        // Get the implemented interface, else we will have Default<Name> instead of <Name>
        var modelingType = Arrays.stream(submodelElement.getClass().getInterfaces())
                .findFirst()
                .map(Class::getSimpleName)
                .orElse("SubmodelElement");

        return elementMapper.mapReferableToAssetBuilder(submodelElement)
                .id(elementMapper.getId(accessUrl, dataAddress))
                .contentType("application/json")
                .properties(Map.of(
                        "embeddedDataSpecifications",
                        Optional.ofNullable(submodelElement.getEmbeddedDataSpecifications())
                                .orElse(List.of(new DefaultEmbeddedDataSpecification())),
                        "semanticId",
                        Optional.ofNullable(submodelElement.getSemanticId())
                                .orElse(new DefaultReference()),
                        "modelingType", modelingType,
                        "value", children))
                .dataAddress(dataAddress)
                .build();
    }

    private <T extends SubmodelElement> Collection<SubmodelElement> getContainerElements(T submodelElement) {
        if (submodelElement instanceof SubmodelElementCollection) {
            return ((SubmodelElementCollection) submodelElement).getValue();
        } else if (submodelElement instanceof SubmodelElementList) {
            return ((SubmodelElementList) submodelElement).getValue();
        } else { // Can not have any child elements...
            return Collections.emptyList();
        }
    }
}
