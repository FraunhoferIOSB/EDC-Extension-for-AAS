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
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

/**
 * This client uses the FA³ST client as backend. The FA³ST client communicates over standardized AAS API calls, so it should be compatible to all
 * standard-conformant AAS repositories.
 */
public class RemoteAasRepositoryClient implements AasRepositoryClient {

    // FA³ST client
    private final AASRepositoryInterface aasRepositoryInterface;
    private final SubmodelRepositoryInterface submodelRepositoryInterface;
    private final ConceptDescriptionRepositoryInterface conceptDescriptionRepositoryInterface;
    private final RemoteAasRepositoryContext context;

    public RemoteAasRepositoryClient(RemoteAasRepositoryContext context) {
        this.context = context;
        HttpClient httpClient = context.getAuthenticationMethod().httpClientBuilderFor().build();

        this.aasRepositoryInterface = new AASRepositoryInterface(context.getUri(), httpClient);
        this.submodelRepositoryInterface = new SubmodelRepositoryInterface(context.getUri(), httpClient);
        this.conceptDescriptionRepositoryInterface = new ConceptDescriptionRepositoryInterface(context.getUri(), httpClient);
    }

    @Override
    public Environment getEnvironment() throws ConnectException, UnauthorizedException {
        try {
            return new DefaultEnvironment.Builder()
                    .assetAdministrationShells(aasRepositoryInterface.getAll())
                    .submodels(submodelRepositoryInterface.getAll())
                    .conceptDescriptions(conceptDescriptionRepositoryInterface.getAll())
                    .build();
        } catch (ForbiddenException | de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException |
                 MethodNotAllowedException unauthorizedException) {
            throw new UnauthorizedException(unauthorizedException);
        } catch (ConnectivityException e) {
            throw new ConnectException(e.getMessage());
        } catch (StatusCodeException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public URI getUri() {
        return context.getUri();
    }

    @Override
    public List<Reference> getReferences() {
        return context.getReferences();
    }

    @Override
    public List<PolicyBinding> getPolicyBindings() {
        return context.getPolicyBindings();
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
