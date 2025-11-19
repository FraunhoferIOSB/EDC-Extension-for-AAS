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
package de.fraunhofer.iosb.app.aas.mapper.referable.identifiable;

import de.fraunhofer.iosb.app.aas.mapper.referable.ReferableMapper;
import de.fraunhofer.iosb.client.AasServerClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


public class IdentifiableMapper extends ReferableMapper {

    public static final String SUBMODEL_ELEMENT_LOCATION = AAS_V30_NAMESPACE + "Submodel/" + "submodelElements";
    private static final String IDENTIFIABLE_NAMESPACE = AAS_V30_NAMESPACE.concat("Identifiable/");


    public IdentifiableMapper(AasServerClient client) {
        super(client);
    }


    public Asset map(Identifiable identifiable) {
        Reference reference = AasUtils.toReference(identifiable);

        var dataAddress = createDataAddress(reference);

        Asset.Builder builder = super.map(identifiable)
                .id(generateId(reference))
                .dataAddress(dataAddress);

        builder.property(IDENTIFIABLE_NAMESPACE.concat("id"), identifiable.getId());

        var admin = identifiable.getAdministration();

        if (admin != null) {
            builder.property(IDENTIFIABLE_NAMESPACE.concat("administration"), getNamespaced(admin));
        }

        return builder.build();
    }
}
