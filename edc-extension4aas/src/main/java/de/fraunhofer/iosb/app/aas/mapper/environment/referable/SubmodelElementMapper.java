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
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Blob;
import org.eclipse.digitaltwin.aas4j.v3.model.File;
import org.eclipse.digitaltwin.aas4j.v3.model.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import java.util.Collection;
import java.util.Collections;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public class SubmodelElementMapper extends ReferableMapper {

    public static final String SMC_CHILDREN_LOCATION = "value";

    /* May contain traces of recursion */
    public <E extends SubmodelElement> Asset map(Reference parent, E submodelElement, AasProvider provider) {
        Asset.Builder assetBuilder = super.map(submodelElement);

        Reference reference = AasUtils.toReference(parent, submodelElement);

        var children = getChildElements(submodelElement).stream()
                .map(elem -> map(reference, elem, provider))
                .toList();

        var dataAddress = createDataAddress(provider, reference);

        String modelingType = submodelElement.getClass().getSimpleName();

        assetBuilder.dataAddress(dataAddress)
                .id(generateId(dataAddress))
                .property("modelingType", modelingType.replace("Default", ""));

        if (!children.isEmpty()) {
            assetBuilder.property(SMC_CHILDREN_LOCATION, children);
        }

        if (submodelElement.getSemanticId() != null && !submodelElement.getSemanticId().getKeys().isEmpty()) {
            assetBuilder.property(AAS_V30_NAMESPACE + "HasSemantics/" + "semanticId", submodelElement.getSemanticId());
        }

        if (submodelElement instanceof Operation operation) {
            assetBuilder.property(AAS_V30_NAMESPACE + "Operation/" + "inputVariables", operation.getInputVariables());
            assetBuilder.property(AAS_V30_NAMESPACE + "Operation/" + "inoutputVariables", operation.getInoutputVariables());
            assetBuilder.property(AAS_V30_NAMESPACE + "Operation/" + "outputVariables", operation.getOutputVariables());
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
        if (submodelElement instanceof SubmodelElementCollection) {
            return ((SubmodelElementCollection) submodelElement).getValue();
        } else if (submodelElement instanceof SubmodelElementList) {
            return ((SubmodelElementList) submodelElement).getValue();
        } else { // Can not have any child elements...
            return Collections.emptyList();
        }
    }
}
