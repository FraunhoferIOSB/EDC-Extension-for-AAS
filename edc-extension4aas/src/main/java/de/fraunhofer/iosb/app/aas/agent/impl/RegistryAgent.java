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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.aas.agent.AasAgent;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Endpoint;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ProtocolInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RegistryAgent extends AasAgent {

    private final AasServiceRegistry aasServiceRegistry;

    /**
     * Class constructor
     *
     * @param aasDataProcessorFactory For reading AAS data.
     * @param aasServiceRegistry      Register AAS services within the registry
     */
    public RegistryAgent(AasDataProcessorFactory aasDataProcessorFactory, AasServiceRegistry aasServiceRegistry) {
        super(aasDataProcessorFactory);
        this.aasServiceRegistry = aasServiceRegistry;
    }

    @Override
    public SelfDescriptionRepository.SelfDescriptionSourceType supportedType() {
        return SelfDescriptionRepository.SelfDescriptionSourceType.REGISTRY;
    }

    @Override
    public PipelineResult<Map<URL, Environment>> apply(URL url) {
        try {
            return PipelineResult.success(readEnvironment(url));
        } catch (IOException e) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getMessage())));
        }
    }

    private Map<URL, Environment> readEnvironment(URL url) throws IOException {

        URL submodelDescriptorsUrl;
        URL shellDescriptorsUrl;
        try {
            submodelDescriptorsUrl = url.toURI().resolve("/api/v3.0/submodel-descriptors").toURL();
            shellDescriptorsUrl = url.toURI().resolve("/api/v3.0/shell-descriptors").toURL();
        } catch (URISyntaxException resolveUriException) {
            throw new EdcException(
                    format("Error while building URLs for reading from the AAS service at %s", url),
                    resolveUriException);
        }

        Map<URL, DefaultEnvironment.Builder> environmentsByUrl = new HashMap<>();

        // TODO somehow, the "submodelDescriptors" field in AASDescriptor is not filled. source name is "submodels"
        var shellDescriptors = readElements(shellDescriptorsUrl, AssetAdministrationShellDescriptor.class);
        var shellEndpointUrlsSorted = sortByHostAndPort(getShellEndpoints(shellDescriptors));

        for (URL shellUrl : shellEndpointUrlsSorted) {
            // Still need to register AAS services for possible data transfer!!!
            aasServiceRegistry.register(shellUrl.toString());
            for (AssetAdministrationShellDescriptor descriptor : shellDescriptors) {
                var baseUrl = getBaseUrl(shellUrl);

                var descriptorAsEnvironment = asEnvironment(descriptor);

                var envBuilder = environmentsByUrl.getOrDefault(baseUrl, new DefaultEnvironment.Builder());

                descriptorAsEnvironment.getAssetAdministrationShells().forEach(envBuilder::assetAdministrationShells);
                descriptorAsEnvironment.getSubmodels().forEach(envBuilder::submodels);

                environmentsByUrl.put(baseUrl, envBuilder);
            }
        }

        var submodelDescriptors = readElements(submodelDescriptorsUrl, SubmodelDescriptor.class);
        var submodelEndpointUrlsSorted = sortByHostAndPort(getSubmodelEndpoints(submodelDescriptors));

        for (URL submodelUrl : submodelEndpointUrlsSorted) {
            // Still need to register AAS services for possible data transfer!!!
            aasServiceRegistry.register(submodelUrl.toString());
            for (SubmodelDescriptor descriptor : submodelDescriptors) {
                var baseUrl = getBaseUrl(submodelUrl);

                var descriptorAsSubmodel = asSubmodel(descriptor);

                var envBuilder = environmentsByUrl.getOrDefault(baseUrl, new DefaultEnvironment.Builder());

                envBuilder.submodels(descriptorAsSubmodel);
                environmentsByUrl.put(baseUrl, envBuilder);
            }
        }

        return environmentsByUrl.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
    }

    private Submodel asSubmodel(SubmodelDescriptor submodelDescriptor) {
        return new DefaultSubmodel.Builder()
                .embeddedDataSpecifications(submodelDescriptor.getAdministration().getEmbeddedDataSpecifications())
                .extensions(submodelDescriptor.getExtensions())
                .semanticId(submodelDescriptor.getSemanticId())
                .supplementalSemanticIds(submodelDescriptor.getSupplementalSemanticId())
                .administration(submodelDescriptor.getAdministration())
                .id(submodelDescriptor.getId())
                .description(submodelDescriptor.getDescription())
                .displayName(submodelDescriptor.getDisplayName())
                .idShort(submodelDescriptor.getIdShort())
                .build();
    }

    /*
    Environment because shell descriptors contain submodel information.
     */
    private Environment asEnvironment(AssetAdministrationShellDescriptor descriptor) {
        var envBuilder = new DefaultEnvironment.Builder();
        var aasBuilder = new DefaultAssetAdministrationShell.Builder()
                .administration(descriptor.getAdministration())
                .assetInformation(new DefaultAssetInformation.Builder()
                        .assetType(descriptor.getAssetType())
                        .assetKind(descriptor.getAssetKind())
                        .specificAssetIds(descriptor.getSpecificAssetIds())
                        .globalAssetId(descriptor.getGlobalAssetId())
                        .build())
                .description(descriptor.getDescription())
                .displayName(descriptor.getDisplayName())
                .extensions(descriptor.getExtensions())
                .idShort(descriptor.getIdShort())
                .id(descriptor.getId());

        descriptor.getSubmodelDescriptors().stream()
                .map(submodelDescriptor -> new DefaultReference.Builder()
                        .keys(new DefaultKey.Builder()
                                .type(KeyTypes.SUBMODEL)
                                .value(submodelDescriptor.getId())
                                .build())
                        .build())
                .forEach(aasBuilder::submodels);

        envBuilder.assetAdministrationShells(aasBuilder.build());

        descriptor.getSubmodelDescriptors().stream()
                .map(submodelDescriptor -> new DefaultSubmodel.Builder()
                        .administration(submodelDescriptor.getAdministration())
                        .description(submodelDescriptor.getDescription())
                        .displayName(submodelDescriptor.getDisplayName())
                        .embeddedDataSpecifications(submodelDescriptor.getAdministration().getEmbeddedDataSpecifications())
                        .extensions(submodelDescriptor.getExtensions())
                        .idShort(submodelDescriptor.getIdShort())
                        .id(submodelDescriptor.getId())
                        .build())
                .forEach(envBuilder::submodels);

        return envBuilder.build();
    }

    private List<URL> sortByHostAndPort(List<String> urls) {
        return urls.stream().map(spec -> {
            try {
                return new URL(spec);
            } catch (MalformedURLException e) {
                // Don't go forward with this registry element
                return null;
            }
        })
                .collect(Collectors.toCollection(ArrayList::new))
                .stream().sorted(Comparator.comparing(URL::getHost).thenComparingInt(URL::getPort)).toList();
    }

    private @NotNull List<String> getSubmodelEndpoints(Collection<SubmodelDescriptor> submodelDescriptors) {
        return submodelDescriptors.stream()
                .map(descriptor -> descriptor.getEndpoints().stream()
                        .filter(endpoint ->
                                endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTPS") ||
                                        endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTP"))
                        .map(Endpoint::get_interface)
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(Collection::stream)
                .toList();
    }

    private @NotNull List<String> getShellEndpoints(Collection<AssetAdministrationShellDescriptor> shellDescriptors) {
        return shellDescriptors.stream()
                .map(descriptor -> descriptor.getEndpoints().stream()
                        .filter(endpoint ->
                                endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTPS") ||
                                        endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTP"))
                        // There is also AAS-REPOSITORY-3.0 which gives us the general /shells endpoint
                        .filter(endpoint -> endpoint.get_interface().equals("AAS-3.0"))
                        .map(Endpoint::getProtocolInformation)
                        .map(ProtocolInformation::getHref)
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(Collection::stream)
                .toList();
    }

    private URL getBaseUrl(URL url) throws MalformedURLException {
        return new URL(url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : ""));
    }

}
