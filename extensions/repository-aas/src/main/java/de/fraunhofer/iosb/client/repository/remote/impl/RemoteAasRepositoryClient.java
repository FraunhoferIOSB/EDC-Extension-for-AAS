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
package de.fraunhofer.iosb.client.repository.remote.impl;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.util.InetTools;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.MethodNotAllowedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.AASRepositoryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.ConceptDescriptionRepositoryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.SubmodelRepositoryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.util.HttpHelper;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;


/**
 * This client uses the FA³ST client as backend. The FA³ST client communicates over standardized AAS API calls, so it should be compatible to all standard-conformant AAS
 * repositories.
 */
public class RemoteAasRepositoryClient implements AasRepositoryClient {

    // FA³ST client
    private final AASRepositoryInterface aasRepositoryInterface;
    private final SubmodelRepositoryInterface submodelRepositoryInterface;
    private final ConceptDescriptionRepositoryInterface conceptDescriptionRepositoryInterface;
    private final RemoteAasRepositoryContext context;
    private boolean shellInterfaceActivated = true;
    private boolean submodelInterfaceActivated = true;
    private boolean conceptDescriptionInterfaceActivated = true;


    public RemoteAasRepositoryClient(RemoteAasRepositoryContext context) {
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

        if (context.isOnlySubmodels()) {
            // Disable shell and concept-description interfaces
            shellInterfaceActivated = false;
            conceptDescriptionInterfaceActivated = false;
        }

        this.aasRepositoryInterface = new AASRepositoryInterface(context.getUri(), httpClient);
        this.submodelRepositoryInterface = new SubmodelRepositoryInterface(context.getUri(), httpClient);
        this.conceptDescriptionRepositoryInterface = new ConceptDescriptionRepositoryInterface(context.getUri(), httpClient);
    }


    @Override
    public Environment getEnvironment() throws ConnectException, UnauthorizedException {
        List<AssetAdministrationShell> shells = null;
        List<Submodel> submodels = null;
        List<ConceptDescription> conceptDescriptions = null;
        try {
            shells = getAas();
            submodels = getSubmodels();
            conceptDescriptions = getConceptDescriptions();
        }
        catch (Exception e) {
            handleException(e);
        }

        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(shells)
                .submodels(submodels)
                .conceptDescriptions(conceptDescriptions)
                .build();
    }


    private void handleException(Exception e) throws ConnectException, UnauthorizedException {
        if (e instanceof ForbiddenException | e instanceof de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException) {
            throw new UnauthorizedException(e);
        }
        else if (e instanceof ConnectivityException) {
            throw new ConnectException(e.getMessage());
        }
        else if (e instanceof StatusCodeException) {
            throw new RuntimeException(e);
        }
    }


    private List<AssetAdministrationShell> getAas() throws StatusCodeException, ConnectivityException {
        if (!shellInterfaceActivated) {
            return List.of();
        }
        try {
            return aasRepositoryInterface.getAll();
        }
        catch (MethodNotAllowedException methodNotAllowedException) {
            shellInterfaceActivated = false;
            return getAas();
        }
    }


    private List<Submodel> getSubmodels() throws StatusCodeException, ConnectivityException {
        if (!submodelInterfaceActivated) {
            return List.of();
        }
        try {
            return submodelRepositoryInterface.getAll();
        }
        catch (MethodNotAllowedException methodNotAllowedException) {
            submodelInterfaceActivated = false;
            return getSubmodels();
        }
    }


    private List<ConceptDescription> getConceptDescriptions() throws StatusCodeException, ConnectivityException {
        if (!conceptDescriptionInterfaceActivated) {
            return List.of();
        }
        try {
            return conceptDescriptionRepositoryInterface.getAll();
        }
        catch (MethodNotAllowedException methodNotAllowedException) {
            conceptDescriptionInterfaceActivated = false;
            return getConceptDescriptions();
        }
    }


    @Override
    public boolean doRegister(Reference reference) {
        return context.doRegister(reference);
    }


    @Override
    public URI getUri() {
        return context.getUri();
    }


    @Override
    public PolicyBinding getPolicyBinding(Reference reference) {
        return context.getPolicyBinding(reference);
    }


    @Override
    public boolean requiresAuthentication() {
        return context.getAuthenticationMethod().getHeader() != null;
    }


    @Override
    public Map<String, String> getHeaders() {
        return Map.ofEntries(context.getAuthenticationMethod().getHeader());
    }


    @Override
    public boolean isAvailable() {
        return InetTools.pingHost(getUri().getHost(), getUri().getPort());
    }
}
