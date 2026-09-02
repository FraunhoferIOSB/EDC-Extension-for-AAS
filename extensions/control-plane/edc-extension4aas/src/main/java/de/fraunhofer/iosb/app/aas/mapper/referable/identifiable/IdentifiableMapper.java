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
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;


/**
 * Maps an AAS {@link Identifiable} (e.g. asset administration shell, submodel or concept description) to an EDC
 * {@link Asset}, assigning a unique asset id and a data address pointing at the element on the AAS server.
 */
public class IdentifiableMapper extends ReferableMapper {

    /**
     * Creates a new identifiable mapper bound to the given AAS server client.
     *
     * @param client Client used to communicate with the AAS server.
     */
    public IdentifiableMapper(AasServerClient client) {
        super(client);
    }


    /**
     * Maps the given identifiable to an EDC asset. The asset id is derived from the element's reference and the data
     * address is built according to the current {@link Configuration} (AAS data plane vs. HTTP data plane).
     *
     * @param identifiable The AAS identifiable to map.
     * @return The mapped EDC asset.
     */
    public Asset map(Identifiable identifiable) {
        Reference reference = AasUtils.toReference(identifiable);

        AasDataAddress dataAddress = createDataAddress(reference);

        Asset.Builder builder = super.map(identifiable)
                .id(generateId(reference));

        if (Configuration.getInstance().useAasDataPlane() && !Configuration.getInstance().isHercules()) {
            builder.dataAddress(dataAddress);
        }
        else {
            builder.dataAddress(dataAddress.asHttpDataAddress());
        }

        return builder.build();
    }
}
