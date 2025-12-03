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
import de.fraunhofer.iosb.app.aas.mapper.util.FilteredJsonSerializer;
import de.fraunhofer.iosb.client.AasServerClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_EXPOSED_FIELDS;


public abstract class ReferableMapper extends ElementMapper {

    public static final String CONTENT_TYPE = "application/json";
    private final FilteredJsonSerializer jsonSerializer = new FilteredJsonSerializer();


    protected ReferableMapper(AasServerClient client) {
        super(client);
    }


    public Asset.Builder map(Referable referable) {

        var assetBuilder = Asset.Builder.newInstance();

        var filterDefaults = jsonSerializer.toMap(referable, DEFAULT_EXPOSED_FIELDS);
        assetBuilder.properties(filterDefaults);

        String[] modelingType = referable.getClass().getAnnotation(IRI.class).value();

        if (modelingType.length > 0) {
            assetBuilder.property(AAS_V30_NAMESPACE.concat("modelingType"), removeAasPrefix(modelingType[0]));
        }

        return assetBuilder.contentType(CONTENT_TYPE);
    }


    private String removeAasPrefix(String s) {
        if (s.startsWith("aas:")) {
            return s.substring("aas:".length());
        }
        return s;
    }
}
