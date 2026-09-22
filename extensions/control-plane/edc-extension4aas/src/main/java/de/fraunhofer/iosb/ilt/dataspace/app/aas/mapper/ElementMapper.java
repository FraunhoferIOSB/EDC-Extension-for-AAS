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
package de.fraunhofer.iosb.ilt.dataspace.app.aas.mapper;

import de.fraunhofer.iosb.ilt.dataspace.app.aas.mapper.util.AssetIdUtil;
import de.fraunhofer.iosb.ilt.dataspace.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.jetbrains.annotations.NotNull;


/**
 * Contains base logic for mapping AAS elements to Assets
 */
public class ElementMapper {

    private final AasServerContext context;


    /**
     * Creates a new element mapper bound to the given AAS server client.
     *
     * @param context Client used to communicate with the AAS server.
     */
    protected ElementMapper(AasServerContext context) {
        this.context = context;
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
        return AssetIdUtil.id(context.getUri().toString(), reference);

    }


    /**
     * Builds an {@link AasDataAddress} pointing at the given reference on the AAS server. If the underlying client
     * requires authentication, the corresponding headers are added to the data address.
     *
     * @param reference Location of the AAS element in its environment.
     * @return A data address pointing at the referenced AAS element.
     */
    protected HttpDataAddress createDataAddress(Reference reference) {
        HttpDataAddress.Builder builder = HttpDataAddress.Builder.newInstance()
                .baseUrl(context.getUri().toString())
                .path(AasDataAddress.pathFromReference(reference));

        if (context.requiresAuthentication()) {
            context.getAuthenticationMethod().decorate(builder);
        }

        return builder.build();
    }
}
