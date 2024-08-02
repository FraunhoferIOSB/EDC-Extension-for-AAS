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
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.EdcException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RegistryAgent extends AasAgent {

    public RegistryAgent(AasDataProcessorFactory aasDataProcessorFactory) {
        super(aasDataProcessorFactory);
    }

    @Override
    public SelfDescriptionRepository.SelfDescriptionSourceType supportedType() {
        return SelfDescriptionRepository.SelfDescriptionSourceType.REGISTRY;
    }

    @Override
    public PipelineResult<Map<String, Environment>> apply(URL url) {
        try {
            return PipelineResult.success(readEnvironment(url));
        } catch (IOException e) {
            return PipelineResult.failure(PipelineFailure.fatal(List.of(e.getMessage())));
        }
    }

    private Map<String, Environment> readEnvironment(URL url) throws IOException {

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

        Map<String, DefaultEnvironment.Builder> environmentsByUrl = new HashMap<>();

        var submodelDescriptors = readElement(submodelDescriptorsUrl, SubmodelDescriptor.class);
        var submodelEndpointUrlsSorted = sortByHostAndPort(getSubmodelEndpoints(submodelDescriptors));

        submodelEndpointUrlsSorted.forEach(submodelUrl -> {
            try {
                readElement(submodelUrl, Submodel.class).forEach(elem ->
                        environmentsByUrl.computeIfAbsent(getBaseUrl(submodelUrl),
                                k -> new DefaultEnvironment.Builder()).submodels(elem)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        var shellDescriptors = readElement(shellDescriptorsUrl, AssetAdministrationShellDescriptor.class);
        var shellEndpointUrlsSorted = sortByHostAndPort(getShellEndpoints(shellDescriptors));

        shellEndpointUrlsSorted.forEach(shellUrl -> {
            try {
                readElement(shellUrl, AssetAdministrationShell.class).forEach(elem ->
                        environmentsByUrl.computeIfAbsent(getBaseUrl(shellUrl),
                                k -> new DefaultEnvironment.Builder()).assetAdministrationShells(elem));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        return environmentsByUrl.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().build()));
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
                        .map(Endpoint::get_interface)
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(Collection::stream)
                .toList();
    }

    private String getBaseUrl(URL url) {
        return url.getProtocol() + "://" + url.getHost() + (url.getPort() != -1 ? ":" + url.getPort() : "");

    }

}
