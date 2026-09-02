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

import de.fraunhofer.iosb.app.aas.mapper.util.AssetIdUtil;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.jetbrains.annotations.NotNull;


/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {

    private final AasServerClient client;

    /**
     * Creates a new element mapper bound to the given AAS server client.
     *
     * @param client Client used to communicate with the AAS server.
     */
    protected ElementMapper(AasServerClient client) {
        this.client = client;
    }


    /**
     * Generates a unique and fixed asset id for the given AAS reference, based on the AAS server location and the
     * reference itself.
     *
     * @param reference Location of the AAS element in its environment.
     * @return A unique and fixed asset id.
     */
    @NotNull
    public String generateId(Reference reference) {
        return AssetIdUtil.id(client.getUri().toString(), reference);

    }


    /**
     * Builds an {@link AasDataAddress} pointing at the given reference on the AAS server. If the underlying client
     * requires authentication, the corresponding headers are added to the data address.
     *
     * @param reference Location of the AAS element in its environment.
     * @return A data address pointing at the referenced AAS element.
     */
    protected AasDataAddress createDataAddress(Reference reference) {
        AasDataAddress.Builder builder = AasDataAddress.Builder.newInstance()
                .baseUrl(client.getUri().toString())
                .reference(reference);

        if (client.requiresAuthentication()) {
            builder.additionalHeaders(client.getHeaders());
        }

        return builder.build();
    }
}
