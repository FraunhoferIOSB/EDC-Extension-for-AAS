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
package de.fraunhofer.iosb.app.aas.mapper.descriptor;

import de.fraunhofer.iosb.app.aas.mapper.ElementMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.Descriptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


/**
 * Abstract class for mapping any AAS registry descriptor to EDC Assets
 */
public abstract class DescriptorMapper extends ElementMapper {
    private static final String DESCRIPTOR_NAMESPACE = AAS_V30_NAMESPACE.concat("/Descriptor/");

    protected Asset.Builder map(Descriptor descriptor) {
        return Asset.Builder.newInstance()
                .contentType("application/json")
                .property(DESCRIPTOR_NAMESPACE.concat("description"), descriptor.getDescription())
                .property(DESCRIPTOR_NAMESPACE.concat("extensions"), descriptor.getExtensions())
                .property(DESCRIPTOR_NAMESPACE.concat("displayName"), descriptor.getDisplayName());
    }
}
