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
package de.fraunhofer.iosb.ilt.app.aas.mapper;

import de.fraunhofer.iosb.ilt.aas.lib.model.AasProvider;
import org.eclipse.digitaltwin.aas4j.v3.model.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Not an ElementMapper since we have a different method signature.
 */
class SubmodelElementMapper {

    private final ElementMapper elementMapper;

    SubmodelElementMapper() {
        this.elementMapper = new ElementMapper();
    }

    /* May contain traces of recursion */
    <E extends SubmodelElement> Asset map(Reference parent, E submodelElement, AasProvider provider) {
        var reference = elementMapper.createReference(submodelElement.getIdShort(), parent);

        var children = getContainerElements(submodelElement).stream()
                .map(elem -> map(reference, elem, provider))
                .toList();

        var dataAddress = elementMapper.createDataAddress(provider, reference);

        // Display the modeling type of the element, i.e. "Property"/"Operation"/...
        // Get the implemented interface, else we will have Default<Name> instead of <Name>
        var modelingType = Arrays.stream(submodelElement.getClass().getInterfaces())
                .findFirst()
                .map(Class::getSimpleName)
                .orElse("SubmodelElement");

        var additionalProperties = new HashMap<>(Map.of(
                "modelingType", modelingType,
                "value", children));

        if (submodelElement.getEmbeddedDataSpecifications() != null) {
            additionalProperties.put("embeddedDataSpecifications", submodelElement.getEmbeddedDataSpecifications());
        }

        if (submodelElement.getSemanticId() != null) {
            additionalProperties.put("semanticId", submodelElement.getSemanticId());
        }

        if (submodelElement instanceof Operation operation) {
            additionalProperties.put("inputVariables", operation.getInputVariables());
            additionalProperties.put("inoutputVariables", operation.getInoutputVariables());
            additionalProperties.put("outputVariables", operation.getOutputVariables());
        }

        return elementMapper.mapReferableToAssetBuilder(submodelElement)
                .id(elementMapper.getId(dataAddress))
                .contentType("application/json")
                .properties(additionalProperties)
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
