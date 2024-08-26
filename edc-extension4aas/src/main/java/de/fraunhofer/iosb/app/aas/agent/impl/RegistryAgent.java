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
import de.fraunhofer.iosb.app.model.aas.registry.Registry;
import de.fraunhofer.iosb.app.model.aas.service.Service;
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
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAdministrativeInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetInformation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEmbeddedDataSpecification;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public class RegistryAgent extends AasAgent<Registry, Map<Service, Environment>> {

    public static final String SHELL_DIRECT_ENDPOINT = "AAS-3.0";
    public static final String SUBMODEL_DIRECT_ENDPOINT = "SUBMODEL-3.0";

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
    public PipelineResult<Map<Service, Environment>> apply(@Nonnull Registry registry) {
        try {
            return readEnvironment(registry);
        } catch (Exception e) {
            return PipelineResult.failure(PipelineFailure.warning(List.of(e.getClass().getName(), e.getMessage())));
        }
    }

    private PipelineResult<Map<Service, Environment>> readEnvironment(Registry registry) throws IOException {
        Map<Service, DefaultEnvironment.Builder> environmentsByUrl = new HashMap<>();

        Result<List<AssetAdministrationShellDescriptor>> shellDescriptors;
        Result<List<SubmodelDescriptor>> submodelDescriptors;
        try {
            shellDescriptors = readElements(registry, registry.getShellDescriptorUrl(), AssetAdministrationShellDescriptor.class);
            submodelDescriptors = readElements(registry, registry.getSubmodelDescriptorUrl(), SubmodelDescriptor.class);
        } catch (EdcException e) {
            // If an exception was raised, produce a fatal result
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getClass().getSimpleName(), e.getMessage())));
        }

        addShellDescriptors(environmentsByUrl, shellDescriptors.getContent());
        addSubmodelDescriptors(environmentsByUrl, submodelDescriptors.getContent());

        Map<Service, Environment> environment = environmentsByUrl.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()
                        .build()));

        if (shellDescriptors.failed() || submodelDescriptors.failed()) {
            return PipelineResult.recoverableFailure(environment,
                    PipelineFailure.warning(
                            Stream.of(shellDescriptors, submodelDescriptors)
                                    .map(AbstractResult::getFailureMessages)
                                    .flatMap(List::stream)
                                    .toList()));

        }
        return PipelineResult.success(environment);
    }

    private void addSubmodelDescriptors(Map<Service, DefaultEnvironment.Builder> environmentsByUrl,
                                        List<SubmodelDescriptor> submodelDescriptors) throws MalformedURLException {
        var submodelEndpointUrlsSorted = sortByHostAndPort(getEndpointUrls(
                submodelDescriptors.stream()
                        .map(SubmodelDescriptor::getEndpoints)
                        .flatMap(Collection::stream)
                        .toList(),
                SUBMODEL_DIRECT_ENDPOINT));

        for (URL submodelUrl : submodelEndpointUrlsSorted) {
            // Still need to register AAS services for possible data transfer!!!
            aasServiceRegistry.register(submodelUrl.toString());
            for (SubmodelDescriptor descriptor : submodelDescriptors) {
                var baseUrl = getBaseUrl(submodelUrl);
                var service = new Service(baseUrl);

                var descriptorAsSubmodel = asSubmodel(descriptor);

                var envBuilder = environmentsByUrl.getOrDefault(service, new DefaultEnvironment.Builder());

                envBuilder.submodels(descriptorAsSubmodel);
                environmentsByUrl.put(service, envBuilder);
            }
        }
    }

    private void addShellDescriptors(Map<Service, DefaultEnvironment.Builder> environmentsByUrl,
                                     List<AssetAdministrationShellDescriptor> shellDescriptors) throws MalformedURLException {
        var shellEndpointUrlsSorted = sortByHostAndPort(getEndpointUrls(
                shellDescriptors.stream()
                        .map(AssetAdministrationShellDescriptor::getEndpoints)
                        .flatMap(Collection::stream)
                        .toList(),
                SHELL_DIRECT_ENDPOINT));

        for (URL shellUrl : shellEndpointUrlsSorted) {
            // Still need to register AAS services for possible data transfer!!!
            aasServiceRegistry.register(shellUrl.toString());
            for (AssetAdministrationShellDescriptor descriptor : shellDescriptors) {
                var baseUrl = getBaseUrl(shellUrl);
                var service = new Service(baseUrl);

                var descriptorAsEnvironment = asEnvironment(descriptor);

                var envBuilder = environmentsByUrl.getOrDefault(service, new DefaultEnvironment.Builder());

                descriptorAsEnvironment.getAssetAdministrationShells().forEach(envBuilder::assetAdministrationShells);
                descriptorAsEnvironment.getSubmodels().forEach(envBuilder::submodels);

                environmentsByUrl.put(service, envBuilder);
            }
        }

    }

    private Submodel asSubmodel(SubmodelDescriptor submodelDescriptor) {
        return new DefaultSubmodel.Builder()
                .embeddedDataSpecifications(
                        Optional.ofNullable(submodelDescriptor.getAdministration())
                                .orElse(new DefaultAdministrativeInformation.Builder()
                                        .embeddedDataSpecifications(new DefaultEmbeddedDataSpecification())
                                        .build())
                                .getEmbeddedDataSpecifications())
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
                .map(this::asSubmodel)
                .forEach(envBuilder::submodels);

        return envBuilder.build();
    }

    private List<URL> sortByHostAndPort(List<String> urls) {
        return urls.stream().map(RegistryAgent::convertToUrl)
                .collect(Collectors.toCollection(ArrayList::new)).stream()
                .sorted(Comparator.comparing(URL::getHost).thenComparingInt(URL::getPort))
                .toList();
    }

    private static URL convertToUrl(String spec) {
        try {
            return new URL(spec);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private @Nonnull List<String> getEndpointUrls(Collection<Endpoint> endpoints, String identifier) {
        return endpoints.stream()
                .filter(endpoint ->
                        endpoint.get_interface().equals(identifier) && (
                                endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTPS") ||
                                        endpoint.getProtocolInformation().getEndpointProtocol().equals("HTTP")))
                .map(Endpoint::getProtocolInformation)
                .map(ProtocolInformation::getHref)
                .filter(Objects::nonNull)
                .toList();
    }

    private URL getBaseUrl(URL url) throws MalformedURLException {
        return new URL(url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : ""));
    }
}
