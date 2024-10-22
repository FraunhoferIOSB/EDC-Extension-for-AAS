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

import de.fraunhofer.iosb.model.aas.AasProvider;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

public class ConceptDescriptionMapper extends ElementMapper implements Mapper<ConceptDescription> {

    @Override
    public Asset map(ConceptDescription conceptDescription, AasProvider provider) {
        var dataAddress = createDataAddress(provider, createReference(KeyTypes.CONCEPT_DESCRIPTION,
                conceptDescription.getId()));

        return mapIdentifiableToAssetBuilder(conceptDescription)
                .id(getId(dataAddress))
                .dataAddress(dataAddress)
                .build();
    }
}
