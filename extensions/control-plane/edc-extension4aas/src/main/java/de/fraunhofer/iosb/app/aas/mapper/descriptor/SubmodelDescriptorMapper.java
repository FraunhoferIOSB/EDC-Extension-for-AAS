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

import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityAttributeObject;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityTypeEnum;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


public class SubmodelDescriptorMapper extends DescriptorMapper {
    private static final String SUBMODEL_DESCRIPTOR_NAMESPACE = AAS_V30_NAMESPACE.concat("/SubmodelDescriptor/");
    private final List<String> supportedProtocols = List.of("HTTP", "HTTPS", "http", "https");

    public Asset map(SubmodelDescriptor descriptor) throws URISyntaxException {
        DataAddress dataAddress = createDataAddress(new URI(getHref(descriptor.getEndpoints())));
        return super.map(descriptor)
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("administration"), descriptor.getAdministration())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("idShort"), descriptor.getIdShort())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("id"), descriptor.getId())
                .property(SUBMODEL_DESCRIPTOR_NAMESPACE.concat("semanticId"), descriptor.getSemanticId())
                .dataAddress(dataAddress)
                .id(generateId(dataAddress))
                .build();
    }


    private String getHref(List<Endpoint> endpoints) {
        return endpoints.stream()
                .filter(endpoint -> endpoint.get_interface().startsWith("SUBMODEL"))
                .filter(endpoint -> endpoint.get_interface().endsWith("3.0"))
                .map(Endpoint::getProtocolInformation)
                .filter(protocolInformation -> supportedProtocols.contains(protocolInformation.getEndpointProtocol()))
                // Remove endpoints which require security
                .filter(protocolInformation -> protocolInformation.getSecurityAttributes().stream()
                        .map(SecurityAttributeObject::getType)
                        .anyMatch(type -> type.equals(SecurityTypeEnum.NONE)))
                .map(ProtocolInformation::getHref)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No suitable endpoints found."));
    }
}
