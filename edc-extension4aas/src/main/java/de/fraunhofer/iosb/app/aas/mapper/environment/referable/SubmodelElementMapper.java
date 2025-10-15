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
package de.fraunhofer.iosb.app.aas.mapper.environment.referable;

import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.type.AasConstants;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Blob;
import org.eclipse.digitaltwin.aas4j.v3.model.File;
import org.eclipse.digitaltwin.aas4j.v3.model.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public class SubmodelElementMapper extends ReferableMapper {

    public static final String SMC_CHILDREN_LOCATION = "value";
    public static final String OPERATION = AasConstants.AAS_V30_NAMESPACE + "Operation";

    /* May contain traces of recursion */
    public <E extends SubmodelElement> Asset map(Reference parent, E submodelElement, AasProvider provider) {
        Reference reference = AasUtils.toReference(parent, submodelElement);
        var dataAddress = createDataAddress(provider, reference);

        Asset.Builder assetBuilder = super.map(submodelElement)
                .dataAddress(dataAddress)
                .id(generateId(dataAddress));

        String[] modelingType = submodelElement.getClass().getAnnotation(IRI.class).value();

        if (modelingType.length > 0) {
            assetBuilder.property(AAS_V30_NAMESPACE.concat("modelingType"), modelingType[0]);
        }

        var children = getChildElements(submodelElement).stream()
                .map(elem -> map(reference, elem, provider))
                .toList();

        if (!children.isEmpty()) {
            assetBuilder.property(SMC_CHILDREN_LOCATION, children);
        }

        if (submodelElement instanceof Operation operation) {
            assetBuilder.property(OPERATION,
                    Map.of(OPERATION.concat("/inputVariables"), getNamespacedList(operation.getInputVariables()),
                            OPERATION.concat("/inoutputVariables"), getNamespacedList(operation.getInoutputVariables()),
                            OPERATION.concat("/outputVariables"), getNamespacedList(operation.getOutputVariables()))
            );
        }

        if (submodelElement instanceof File file) {
            assetBuilder.contentType(file.getContentType());
        } else if (submodelElement instanceof Blob blob) {
            assetBuilder.contentType(blob.getContentType());
        }

        return assetBuilder
                .build();
    }

    private <T extends SubmodelElement> Collection<SubmodelElement> getChildElements(T submodelElement) {
        if (submodelElement instanceof SubmodelElementCollection collection) {
            return collection.getValue();
        } else if (submodelElement instanceof SubmodelElementList list) {
            return list.getValue();
        } else { // Can not have any child elements...
            return Collections.emptyList();
        }
    }
}
