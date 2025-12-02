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
package de.fraunhofer.iosb.app.handler.aas.registry;

import de.fraunhofer.iosb.app.aas.mapper.util.AssetIdUtil;
import de.fraunhofer.iosb.app.handler.aas.RemoteAasHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.registry.AasRegistryClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Descriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityAttributeObject;
import org.eclipse.digitaltwin.aas4j.v3.model.SecurityTypeEnum;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static de.fraunhofer.iosb.constants.AasConstants.SUPPORTED_AAS_VERSION;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;


/**
 * Handles synchronization of AAS registries shell/submodel descriptors and EDC stores.
 */
public class RemoteAasRegistryHandler extends RemoteAasHandler<AasRegistryClient> {
    private static final List<String> SUPPORTED_PROTOCOLS = List.of("HTTP", "HTTPS", "http", "https");


    /**
     * Class constructor.
     *
     * @param monitor Logging.
     * @param client Communication with AAS registry.
     * @param edcStoreHandler EDC API.
     * @throws UnauthorizedException Initial communication with AAS registry failed due to being unauthorized.
     * @throws ConnectException Initial communication with AAS registry failed due to a connection exception.
     */
    public RemoteAasRegistryHandler(Monitor monitor, AasRegistryClient client, EdcStoreHandler edcStoreHandler) throws UnauthorizedException,
            ConnectException {
        super(monitor, client, edcStoreHandler);
    }


    @Override
    protected Environment getEnvironment() throws UnauthorizedException, ConnectException {
        List<AssetAdministrationShell> shells = List.of();
        List<Submodel> submodels = List.of();
        try {
            shells = getAllAsShells().values().stream().toList();
        }
        catch (NoEndpointException e) {
            monitor.severe(String.format("No descriptor endpoints for one shell descriptor at registry %s", client.getUri()), e);
        }
        try {
            submodels = getAllAsSubmodels().values().stream().toList();
        }
        catch (NoEndpointException e) {
            monitor.severe(String.format("No descriptor endpoints for one submodel descriptor at registry %s", client.getUri()), e);
        }

        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(shells)
                .submodels(submodels)
                .build();
    }


    @Override
    protected Consumer<Identifiable> getSelfDescriptionIdentifiableMapper() {
        return this::registryIdentifiableVisitor;
    }


    private void registryIdentifiableVisitor(Identifiable identifiable) {
        // Find suitable URI
        String uri = registeredAssets.entrySet().stream()
                .filter(entry -> entry.getKey().referredElement().equals(AasUtils.toReference(identifiable)))
                .map(Map.Entry::getValue)
                .map(Asset::getDataAddress)
                .map(da -> (String) da.getProperty(BASE_URL))
                .filter(Objects::nonNull)
                .findAny()
                .orElseThrow();

        identifiable.setExtensions(List.of(buildExtension(AssetIdUtil.id(uri, identifiable))));
    }


    private Map<URI, Submodel> getAllAsSubmodels() throws UnauthorizedException, ConnectException, NoEndpointException {
        List<DefaultSubmodelDescriptor> submodelDescriptors = new ArrayList<>(client.getSubmodelDescriptors());

        client.getShellDescriptors().stream()
                .map(DefaultAssetAdministrationShellDescriptor::getSubmodelDescriptors)
                .flatMap(Collection::stream)
                .map(DefaultSubmodelDescriptor.class::cast)
                .forEach(submodelDescriptors::add);

        Map<URI, Submodel> collect = new HashMap<>();
        for (var descriptor: submodelDescriptors) {
            collect.put(getAppropriateEndpoint(descriptor.getEndpoints(), SubmodelDescriptor.class),
                    asSubmodel(descriptor));
        }
        return collect;
    }


    private Submodel asSubmodel(SubmodelDescriptor descriptor) {
        return new DefaultSubmodel.Builder()
                .administration(descriptor.getAdministration())
                .description(descriptor.getDescription())
                .displayName(descriptor.getDisplayName())
                .extensions(descriptor.getExtensions())
                .id(descriptor.getId())
                .idShort(descriptor.getIdShort())
                .semanticId(descriptor.getSemanticId())
                .supplementalSemanticIds(descriptor.getSupplementalSemanticId())
                .build();
    }


    private Map<URI, AssetAdministrationShell> getAllAsShells() throws UnauthorizedException, ConnectException, NoEndpointException {
        List<DefaultAssetAdministrationShellDescriptor> shellDescriptors = client.getShellDescriptors();
        Map<URI, AssetAdministrationShell> collect = new HashMap<>();
        for (var descriptor: shellDescriptors) {
            collect.put(getAppropriateEndpoint(descriptor.getEndpoints(), AssetAdministrationShellDescriptor.class),
                    asAssetAdministrationShell(descriptor));
        }
        return collect;
    }


    private AssetAdministrationShell asAssetAdministrationShell(AssetAdministrationShellDescriptor descriptor) {
        return new DefaultAssetAdministrationShell.Builder()
                .administration(descriptor.getAdministration())
                .assetInformation(new DefaultAssetInformation.Builder()
                        .assetKind(descriptor.getAssetKind())
                        .assetType(descriptor.getAssetType())
                        .globalAssetId(descriptor.getGlobalAssetId())
                        .specificAssetIds(descriptor.getSpecificAssetIds())
                        .build())
                .description(descriptor.getDescription())
                .displayName(descriptor.getDisplayName())
                .extensions(descriptor.getExtensions())
                .id(descriptor.getId())
                .idShort(descriptor.getIdShort())
                .build();
    }


    private <T extends Descriptor> URI getAppropriateEndpoint(List<Endpoint> endpoints, Class<T> clazz) throws NoEndpointException {
        String interfaceShortNames;
        if (clazz.equals(AssetAdministrationShellDescriptor.class)) {
            interfaceShortNames = "AAS-".concat(SUPPORTED_AAS_VERSION);
        }
        else if (clazz.equals(SubmodelDescriptor.class)) {
            interfaceShortNames = "SUBMODEL-".concat(SUPPORTED_AAS_VERSION);
        }
        else {
            throw new IllegalArgumentException(String.format("Descriptor type not recognized: %s", clazz.getSimpleName()));
        }
        return endpoints.stream()
                .filter(ep -> interfaceShortNames.equalsIgnoreCase(ep.get_interface()))
                .map(Endpoint::getProtocolInformation)
                .filter(protocolInformation -> SUPPORTED_PROTOCOLS.contains(protocolInformation.getEndpointProtocol()))
                // Remove endpoints which require security
                .filter(protocolInformation ->
                        protocolInformation.getSecurityAttributes().isEmpty() ||
                                protocolInformation.getSecurityAttributes().stream()
                                        .map(SecurityAttributeObject::getType)
                                        .anyMatch(type -> type.equals(SecurityTypeEnum.NONE)))
                .map(ProtocolInformation::getHref)
                .map(URI::create)
                .findAny().orElseThrow(() -> new NoEndpointException("No suitable endpoints found."));
    }
}
