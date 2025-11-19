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
package de.fraunhofer.iosb.client.registry;

import de.fraunhofer.iosb.aas.lib.util.InetTools;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.MethodNotAllowedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.AASRegistryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.SubmodelRegistryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.util.HttpHelper;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelDescriptor;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;


public class AasRegistryClient implements AasServerClient {

    // FA³ST client
    private final AASRegistryInterface aasRegistryInterface;
    private final SubmodelRegistryInterface submodelRegistryInterface;
    private final AasRegistryContext context;


    public AasRegistryClient(AasRegistryContext context) {
        this.context = context;

        HttpClient.Builder httpClientBuilder = context.getAuthenticationMethod()
                .httpClientBuilderFor();

        if (context.allowSelfSigned()) {
            httpClientBuilder.sslContext(HttpHelper.newTrustAllCertificatesClient().sslContext());
        }

        // Version 1.1 fixes compatibility errors
        HttpClient httpClient = httpClientBuilder
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        // TODO when client gets builder, revise this
        this.aasRegistryInterface = new AASRegistryInterface(context.getUri(), httpClient);
        this.submodelRegistryInterface = new SubmodelRegistryInterface(context.getUri(), httpClient);
    }


    @Override
    public boolean isAvailable() {
        return InetTools.pingHost(context.getUri().getHost(), context.getUri().getPort());
    }


    @Override
    public boolean requiresAuthentication() {
        return context.getAuthenticationMethod().getHeader() != null;
    }


    @Override
    public Map<String, String> getHeaders() {
        return Map.ofEntries(context.getAuthenticationMethod().getHeader());
    }


    /**
     * Get all AAS descriptors published by the registry.
     *
     * @return List of AAS descriptors as published by the registry.
     * @throws UnauthorizedException A call to this registry was unauthorized.
     * @throws ConnectException A call to this registry was not possible due to a connection issue.
     */
    public List<DefaultAssetAdministrationShellDescriptor> getShellDescriptors() throws UnauthorizedException, ConnectException {
        try {
            return aasRegistryInterface.getAll();
        }
        catch (ForbiddenException | de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException |
                MethodNotAllowedException unauthorizedException) {
            throw new UnauthorizedException(unauthorizedException);
        }
        catch (ConnectivityException | de.fraunhofer.iosb.ilt.faaast.client.exception.NotFoundException e) {
            throw new ConnectException(e.getMessage());
        }
        catch (StatusCodeException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Get all submodel descriptors published by the registry.
     *
     * @return List of submodel descriptors as published by the registry.
     * @throws UnauthorizedException A call to this registry was unauthorized.
     * @throws ConnectException A call to this registry was not possible due to a connection issue.
     */
    public List<DefaultSubmodelDescriptor> getSubmodelDescriptors() throws UnauthorizedException, ConnectException {
        try {
            return submodelRegistryInterface.getAll();
        }
        catch (ForbiddenException | de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException |
                MethodNotAllowedException unauthorizedException) {
            throw new UnauthorizedException(unauthorizedException);
        }
        catch (ConnectivityException e) {
            throw new ConnectException(e.getMessage());
        }
        catch (StatusCodeException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public URI getUri() {
        return context.getUri();
    }
}
