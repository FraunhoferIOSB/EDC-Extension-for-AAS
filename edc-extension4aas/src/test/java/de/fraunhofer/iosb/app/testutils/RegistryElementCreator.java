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
package de.fraunhofer.iosb.app.testutils;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetKind;
import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEndpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSpecificAssetId;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;

import java.util.List;
import java.util.function.Function;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getAdministrativeInformation;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getExtensions;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getLangStringNameType;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getLangStringTextType;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getReference;
import static de.fraunhofer.iosb.app.testutils.AasCreator.uuid;

public class RegistryElementCreator {


    private RegistryElementCreator() {
        throw new RuntimeException("Utility class");
    }


    public static SubmodelDescriptor getEmptySubmodelDescriptor() {
        return new DefaultSubmodelDescriptor.Builder()
                .endpoints(getEndpoints(uuid(), "SUBMODEL"))
                .build();
    }

    public static SubmodelDescriptor getSubmodelDescriptor() {
        return getSubmodelDescriptor(uuid());
    }

    public static SubmodelDescriptor getSubmodelDescriptor(String id) {
        return new DefaultSubmodelDescriptor.Builder()
                .administration(getAdministrativeInformation(id))
                .endpoints(getEndpoints(id, "SUBMODEL"))
                .id(id)
                .idShort(id)
                .description(getLangStringTextType(id))
                .displayName(getLangStringNameType())
                .extensions(getExtensions(id))
                .semanticId(getReference(id))
                .build();
    }

    public static AssetAdministrationShellDescriptor getEmptyShellDescriptor() {
        return new DefaultAssetAdministrationShellDescriptor.Builder()
                .endpoints(getEndpoints(uuid(), "AAS"))
                .build();
    }

    public static AssetAdministrationShellDescriptor getShellDescriptor() {
        return getShellDescriptor(uuid());
    }

    public static AssetAdministrationShellDescriptor getShellDescriptor(String id) {
        Function<String, String> nameSupplier = str -> str.concat(" shell descriptor");
        return new DefaultAssetAdministrationShellDescriptor.Builder()
                .administration(getAdministrativeInformation(id))
                .assetKind(AssetKind.INSTANCE)
                .assetType(nameSupplier.apply("Asset Type"))
                .endpoints(getEndpoints(id, "AAS"))
                .globalAssetId(nameSupplier.apply("Global Asset Id"))
                .id(id)
                .idShort(id)
                .specificAssetIds(new DefaultSpecificAssetId.Builder().build())
                .submodelDescriptors(new DefaultSubmodelDescriptor.Builder().build())
                .description(getLangStringTextType(id))
                .displayName(getLangStringNameType())
                .extensions(getExtensions(id))
                .build();
    }

    private static List<Endpoint> getEndpoints(String id, String type) {
        return List.of(
                new DefaultEndpoint.Builder()
                        ._interface("%s-3.0".formatted(type))
                        .protocolInformation(new DefaultProtocolInformation.Builder()
                                .href("https://localhost:12345")
                                .endpointProtocol("HTTPS")
                                .endpointProtocolVersion("4.2")
                                .subprotocol(id)
                                .build())
                        .build(),
                new DefaultEndpoint.Builder()
                        ._interface("%s-REPOSITORY-3.0".formatted(type))
                        .protocolInformation(new DefaultProtocolInformation.Builder()
                                .href("http://localhost:420")
                                .endpointProtocol("HTTP")
                                .endpointProtocolVersion("1.0")
                                .subprotocol(id)
                                .build())
                        .build(),
                new DefaultEndpoint.Builder()
                        ._interface("FTP")
                        .protocolInformation(new DefaultProtocolInformation.Builder()
                                .href("http://localhost:420")
                                .endpointProtocol("HTTP")
                                .endpointProtocolVersion("0.98")
                                .subprotocol(id)
                                .build())
                        .build()
        );
    }

}
