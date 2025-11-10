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

import de.fraunhofer.iosb.app.aas.mapper.ElementMapper;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.model.context.AasServerContext;
import org.eclipse.digitaltwin.aas4j.v3.model.HasSemantics;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;

public abstract class ReferableMapper extends ElementMapper {

    public static final String CONTENT_TYPE = "application/json";
    private static final String REFERABLE_NAMESPACE = AAS_V30_NAMESPACE.concat("Referable/");

    protected ReferableMapper(AasServerClient client) {
        super(client);
    }

    public Asset.Builder map(Referable referable) {
        var assetBuilder = Asset.Builder.newInstance();

        if (referable.getIdShort() != null && !referable.getIdShort().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("idShort"), referable.getIdShort());
        }

        if (referable.getDisplayName() != null && !referable.getDisplayName().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("displayName"), getNamespacedList(referable.getDisplayName()));
        }

        if (referable.getDescription() != null && !referable.getDescription().isEmpty()) {
            assetBuilder.property(REFERABLE_NAMESPACE.concat("description"), getNamespacedList(referable.getDescription()));
        }

        if (referable instanceof HasSemantics semanticsHavingIdentifiable &&
                semanticsHavingIdentifiable.getSemanticId() != null &&
                !semanticsHavingIdentifiable.getSemanticId().getKeys().isEmpty()) {
            assetBuilder.property(AAS_V30_NAMESPACE + "HasSemantics/" + "semanticId", getNamespaced(semanticsHavingIdentifiable.getSemanticId()));
        }

        return assetBuilder.contentType(CONTENT_TYPE);
    }

}
