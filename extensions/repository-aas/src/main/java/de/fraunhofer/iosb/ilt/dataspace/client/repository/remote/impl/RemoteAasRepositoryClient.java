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
package de.fraunhofer.iosb.ilt.dataspace.client.repository.remote.impl;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.BasicAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.BearerAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.util.InetTools;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.RemoteClientContext;
import de.fraunhofer.iosb.ilt.faaast.client.exception.BadRequestException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.MethodNotAllowedException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.AASRepositoryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.ConceptDescriptionRepositoryInterface;
import de.fraunhofer.iosb.ilt.faaast.client.interfaces.SubmodelRepositoryInterface;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.security.Vault;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 * This client uses the FA³ST client as backend. The FA³ST client communicates over standardized AAS API calls, so it
 * should be compatible to all standard-conformant AAS
 * repositories.
 */
public class RemoteAasRepositoryClient implements AasRepositoryClient {

    // FA³ST client
    private final AASRepositoryInterface aasRepositoryInterface;
    private final SubmodelRepositoryInterface submodelRepositoryInterface;
    private final ConceptDescriptionRepositoryInterface conceptDescriptionRepositoryInterface;
    private final RemoteClientContext context;
    private boolean shellInterfaceActivated = true;
    private boolean submodelInterfaceActivated = true;
    private boolean conceptDescriptionInterfaceActivated = true;


    /**
     * Constructor of the class.
     *
     * @param vault The vault holding credentials for authentication.
     * @param context The context of the AAS repository, i.e. information needed to communicate with it.
     */
    public RemoteAasRepositoryClient(Vault vault, RemoteClientContext context) {
        this.context = context;

        var aasRepoInterfaceBuilder = new AASRepositoryInterface.Builder()
                .endpoint(context.getUri());
        var submodelRepoInterfaceBuilder = new SubmodelRepositoryInterface.Builder()
                .endpoint(context.getUri());
        var conceptDescriptionRepoInterfaceBuilder = new ConceptDescriptionRepositoryInterface.Builder()
                .endpoint(context.getUri());

        AuthenticationMethod authMethod = context.getAuthenticationMethod();

        if (authMethod instanceof BasicAuth || authMethod instanceof BearerAuth) {
            aasRepoInterfaceBuilder.authenticationHeaderProvider(() -> authMethod.getValue(vault));
            submodelRepoInterfaceBuilder.authenticationHeaderProvider(() -> authMethod.getValue(vault));
            conceptDescriptionRepoInterfaceBuilder.authenticationHeaderProvider(() -> authMethod.getValue(vault));
        }
        else {
            var customHttpClient = authMethod.httpClientBuilderFor(vault).version(HttpClient.Version.HTTP_1_1);
            aasRepoInterfaceBuilder.customHttpClientBuilder(customHttpClient);
            submodelRepoInterfaceBuilder.customHttpClientBuilder(customHttpClient);
            conceptDescriptionRepoInterfaceBuilder.customHttpClientBuilder(customHttpClient);
        }

        if (context.allowSelfSigned()) {
            aasRepoInterfaceBuilder.useTrustAllHttpClient();
            submodelRepoInterfaceBuilder.useTrustAllHttpClient();
            conceptDescriptionRepoInterfaceBuilder.useTrustAllHttpClient();
        }

        this.aasRepositoryInterface = aasRepoInterfaceBuilder.build();
        this.submodelRepositoryInterface = submodelRepoInterfaceBuilder.build();
        this.conceptDescriptionRepositoryInterface = conceptDescriptionRepoInterfaceBuilder.build();
    }


    @Override
    public Environment getEnvironment() throws ConnectivityException, StatusCodeException {
        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(getAas())
                .submodels(getSubmodels())
                .conceptDescriptions(getConceptDescriptions())
                .build();
    }


    @Override
    public URI getUri() {
        return context.getUri();
    }


    @Override
    public boolean isAvailable() {
        return InetTools.pingHost(getUri().getHost(), getUri().getPort());
    }


    private List<AssetAdministrationShell> getAas() throws StatusCodeException, ConnectivityException {
        return getIdentifiableList(() -> shellInterfaceActivated,
                aasRepositoryInterface::getAll,
                (b) -> shellInterfaceActivated = b);
    }


    private List<Submodel> getSubmodels() throws StatusCodeException, ConnectivityException {
        return getIdentifiableList(() -> submodelInterfaceActivated,
                submodelRepositoryInterface::getAll,
                (b) -> submodelInterfaceActivated = b);
    }


    private List<ConceptDescription> getConceptDescriptions() throws StatusCodeException, ConnectivityException {
        return getIdentifiableList(() -> conceptDescriptionInterfaceActivated,
                conceptDescriptionRepositoryInterface::getAll,
                (b) -> conceptDescriptionInterfaceActivated = b);
    }


    // Rethrow exceptions
    @FunctionalInterface
    private interface ThrowingListSupplier<T> {
        List<T> get() throws StatusCodeException, ConnectivityException;
    }


    private <T extends Identifiable> List<T> getIdentifiableList(Supplier<Boolean> activated, ThrowingListSupplier<T> identifiableSupplier, Consumer<Boolean> deactivator)
            throws StatusCodeException, ConnectivityException {
        if (!activated.get()) {
            return List.of();
        }
        try {
            return identifiableSupplier.get();
        }
        catch (BadRequestException | ForbiddenException | MethodNotAllowedException | UnauthorizedException methodNotAllowedException) {
            deactivator.accept(false);
            return List.of();
        }
    }
}
