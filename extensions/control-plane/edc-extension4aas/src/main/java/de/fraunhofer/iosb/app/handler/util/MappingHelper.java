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
package de.fraunhofer.iosb.app.handler.util;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;


/**
 * Mapping of AAS elements to IDS Assets.
 */
public abstract class MappingHelper {

    /**
     * Maps an environment of AAS elements to T. The mapping rules are defined by the identifiableMapper and elementMapper. The identifiableMapper will map all AAS, submodels and
     * concept descriptions, while the element mapper will map all submodel elements.
     *
     * @param environment The environment containing the elements to map.
     * @param identifiableMapper The identifiable mapper used to map all identifiable objects.
     * @param elementMapper Used to map all referable but not identifiable objects.
     * @param <T> The mappers will have a target type, most likely {@link org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset Asset}. AAS elements will be mapped
     *         onto that type.
     * @return Map of references to an element and its mapped counterpart.
     */
    public static <T> Map<Reference, T> map(Environment environment, Function<Identifiable, T> identifiableMapper,
                                            BiFunction<Reference, SubmodelElement, T> elementMapper) {
        Map<Reference, T> result = new HashMap<>();

        environment.getAssetAdministrationShells().stream()
                .map(shell -> mapIdentifiable(shell, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        environment.getConceptDescriptions().stream()
                .map(conceptDescription -> mapIdentifiable(conceptDescription, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        environment.getSubmodels().stream()
                .map(submodel -> mapIdentifiable(submodel, identifiableMapper, elementMapper))
                .forEach(result::putAll);

        return result;
    }


    private static <T> Map<Reference, T> mapIdentifiable(Identifiable identifiable,
                                                         Function<Identifiable, T> mappingFunction,
                                                         BiFunction<Reference, SubmodelElement, T> elementMappingFunction) {
        Map<Reference, T> filteredElements = new HashMap<>();

        Reference identifiableReference = AasUtils.toReference(identifiable);

        if (identifiable instanceof Submodel submodel) {
            submodel.getSubmodelElements().stream()
                    .map(element -> mapSubmodelElement(identifiableReference, element, elementMappingFunction))
                    .forEach(filteredElements::putAll);
        }

        filteredElements.put(identifiableReference, mappingFunction.apply(identifiable));

        return filteredElements;
    }


    private static <T> Map<Reference, T> mapSubmodelElement(Reference parent, SubmodelElement submodelElement,
                                                            BiFunction<Reference, SubmodelElement, T> mappingFunction) {
        Map<Reference, T> result = new HashMap<>();

        Reference submodelElementReference = AasUtils.toReference(parent, submodelElement);

        if (submodelElement instanceof SubmodelElementList list) {
            List<SubmodelElement> children = list.getValue();
            // AASd-120 - aware
            for (int i = 0; i < children.size(); i++) {
                SubmodelElement child = children.get(i);
                child.setIdShort(String.valueOf(i));
                result.putAll(mapSubmodelElement(submodelElementReference, child, mappingFunction));
            }
        }
        else if (submodelElement instanceof SubmodelElementCollection collection) {
            collection.getValue().stream()
                    .map(child -> mapSubmodelElement(submodelElementReference, child, mappingFunction))
                    .forEach(result::putAll);
        }

        result.put(submodelElementReference, mappingFunction.apply(parent, submodelElement));

        return result;
    }
}
