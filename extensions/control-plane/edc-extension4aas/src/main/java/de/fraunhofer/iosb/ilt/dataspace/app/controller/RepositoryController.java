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
package de.fraunhofer.iosb.ilt.dataspace.app.controller;

import de.fraunhofer.iosb.ilt.dataspace.app.controller.dto.LocalRepositoryDTO;
import de.fraunhofer.iosb.ilt.dataspace.app.controller.dto.RegisteredAssetIdsDTO;
import de.fraunhofer.iosb.ilt.dataspace.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.ilt.dataspace.app.executor.VariableRateScheduler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.RemoteHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.RemoteAasHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.event.EventDrivenRepositoryHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.event.impl.LocalFaaastRepositoryHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.period.impl.RemoteAasRepositoryHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.ilt.dataspace.model.config.impl.faaast.FaaastRepositoryConfig;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote.RemoteAasRepositoryContext;
import de.fraunhofer.iosb.ilt.dataspace.repository.AasRepositoryManager;
import de.fraunhofer.iosb.ilt.dataspace.repository.impl.faaast.FaaastRepositoryManager;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ForbiddenException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.util.EncodingHelper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.web.spi.exception.BadGatewayException;
import org.eclipse.edc.web.spi.exception.NotAuthorizedException;

import java.net.URI;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;


/**
 * Handles requests regarding the Asset Administration Shells registered to this extension
 */
@Consumes(APPLICATION_JSON)
@Path("/")
public class RepositoryController extends AbstractAasServerController {

    private static final String REPOSITORY_PATH = "repository";
    private static final String LOCAL_REPOSITORY_PATH = REPOSITORY_PATH + "/local";
    private static final String REMOTE_REPOSITORY_PATH = REPOSITORY_PATH + "/remote";

    private final AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager;


    /**
     * Creates a new repository controller.
     *
     * @param monitor Monitor used for log outputs.
     * @param aasServerStore AAS server store to store AAS servers.
     * @param hostname Hostname used to expose internally started repositories.
     * @param edcStoreHandler EDC store handler to manage registration of EDC assets, policies and contracts.
     * @param vault Vault used to get secrets for authentication at AAS servers.
     * @param oauth2Client OAuth2 client to get tokens for authentication at AAS servers.
     */
    public RepositoryController(Monitor monitor, AasServerStore aasServerStore,
                                Hostname hostname,
                                EdcStoreHandler edcStoreHandler,
                                Vault vault, Oauth2Client oauth2Client) {
        super(monitor, aasServerStore, new VariableRateScheduler(1, monitor), edcStoreHandler, vault, oauth2Client);
        // Use FA³ST to start repositories internally.
        this.aasRepositoryManager = new FaaastRepositoryManager(monitor, hostname);
    }


    /**
     * Starts a FA³ST service internally and registers it to the EDC.
     *
     * @param localRepositoryDTO Request DTO containing environment path, port and config path
     * @return the registered AAS elements and their EDC asset counterparts.
     * @throws WebApplicationException malformed request
     */
    @POST
    @Path(LOCAL_REPOSITORY_PATH)
    @Produces(TEXT_PLAIN)
    public RegisteredAssetIdsDTO register(LocalRepositoryDTO localRepositoryDTO) {
        monitor.debug(String.format("Starting FA³ST repository with model file at %s.", localRepositoryDTO.modelPath()));
        FaaastRepositoryConfig config = localRepositoryDTO.asConfig();

        LocalFaaastRepositoryContext context = aasRepositoryManager.startRepository(config);

        LocalFaaastRepositoryHandler handler;
        try {
            handler = new LocalFaaastRepositoryHandler(monitor, context, vault, edcStoreHandler);
        }
        catch (UnauthorizedException | ForbiddenException unauthorizedException) {
            monitor.warning(String.format(UNAUTHORIZED_EXCEPTION_TEMPLATE, context.getUri()), unauthorizedException);
            throw new NotAuthorizedException(String.format(UNAUTHORIZED_EXCEPTION_TEMPLATE, context.getUri()));
        }
        catch (ConnectivityException | StatusCodeException connectException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, context.getUri()), connectException);
            throw new BadGatewayException(String.format(CONNECT_EXCEPTION_TEMPLATE, context.getUri()));
        }

        aasServerStore.put(context.getUri(), handler);

        return new RegisteredAssetIdsDTO(context.getUri(), handler.registeredAssetIds());
    }


    /**
     * Register an AAS service at the EDC.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     * @return the registered AAS elements and their EDC asset counterparts.
     * @throws WebApplicationException malformed request
     */
    @POST
    @Path(REMOTE_REPOSITORY_PATH)
    public RegisteredAssetIdsDTO register(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        monitor.debug(String.format("Registering AAS repository with URL %s.", remoteAasRepositoryContextDTO.url()));
        if (aasServerStore.isStored(remoteAasRepositoryContextDTO.url())) {
            throw new WebApplicationException(String.format(EXISTS_TEMPLATE, remoteAasRepositoryContextDTO.url()), Response.Status.CONFLICT);
        }

        RemoteAasRepositoryContext context = remoteAasRepositoryContextDTO.asContext(vault, oauth2Client);

        RemoteAasRepositoryHandler handler;
        try {
            handler = new RemoteAasRepositoryHandler(monitor, vault, context, edcStoreHandler);
        }
        catch (UnauthorizedException | ForbiddenException unauthorizedException) {
            monitor.warning(String.format(UNAUTHORIZED_EXCEPTION_TEMPLATE, context.getUri()), unauthorizedException);
            throw new NotAuthorizedException(String.format(UNAUTHORIZED_EXCEPTION_TEMPLATE, context.getUri()));
        }
        catch (ConnectivityException | StatusCodeException connectException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, context.getUri()), connectException);
            throw new BadGatewayException(String.format(CONNECT_EXCEPTION_TEMPLATE, context.getUri()));
        }

        aasServerStore.put(context.getUri(), handler);
        scheduler.addRunnable(handler);

        return new RegisteredAssetIdsDTO(context.getUri(), handler.registeredAssetIds());
    }


    /**
     * Find registered assets for a registered AAS repository.
     *
     * @param aasRepositoryUrlB64Encoded The base64-urlencoded url of the registered url
     * @return the registered AAS elements and their EDC asset counterparts.
     * @throws NotFoundException if the URL was not found.
     */
    @GET
    @Path(REPOSITORY_PATH + "/{url}")
    public RegisteredAssetIdsDTO findByUrl(@PathParam("url") String aasRepositoryUrlB64Encoded) {
        String uriString = EncodingHelper.base64UrlDecode(aasRepositoryUrlB64Encoded);
        monitor.debug(String.format("Getting AAS repository for URL %s.", EncodingHelper.base64UrlDecode(aasRepositoryUrlB64Encoded)));

        URI uri = URI.create(uriString);

        if (!aasServerStore.isStored(uri)) {
            throw new NotFoundException("AAS Repository with url %s not found in registered AAS repositories");
        }

        return aasServerStore.get(uri)
                .filter(RemoteAasHandler.class::isInstance)
                .map(h -> new RegisteredAssetIdsDTO(uri, h.registeredAssetIds()))
                .orElseThrow(() -> new NotFoundException("AAS Repository with url %s not found in registered AAS repositories"));
    }


    /**
     * Update the to-register assets of a registered repository.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     * @return "No Content" if the operation was successful.
     * @throws NotFoundException if the URL was not found.
     */
    @PUT
    @Path(REPOSITORY_PATH)
    public Response updateRegisteredRepository(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        monitor.debug(String.format("Updating registered AAS repository with URL %s.", remoteAasRepositoryContextDTO.url()));

        if (!aasServerStore.isStored(remoteAasRepositoryContextDTO.url())) {
            throw new NotFoundException("AAS Repository with url %s not found in registered AAS repositories.");
        }

        aasServerStore.get(remoteAasRepositoryContextDTO.url())
                .filter(h -> h instanceof RemoteAasHandler<?, ?> || h instanceof AasRepositoryHandler<?, ?>)
                .orElseThrow(() -> new NotFoundException("AAS Repository with url %s not found in registered AAS repositories"))
                .updatePolicyBindings(remoteAasRepositoryContextDTO.policyBindings());

        return Response.noContent().build();
    }


    /**
     * Update the to-register assets of a registered repository. Does not remove existing bindings.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     * @return "No Content" if the operation was successful.
     * @throws NotFoundException if the URL was not found.
     */
    @PATCH
    @Path(REPOSITORY_PATH)
    public Response patchRegisteredRepository(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        monitor.debug(String.format("Patching registered AAS repository with URL %s.", remoteAasRepositoryContextDTO.url()));

        if (!aasServerStore.isStored(remoteAasRepositoryContextDTO.url())) {
            throw new NotFoundException("AAS Repository with url %s not found in registered AAS repositories.");
        }

        aasServerStore.get(remoteAasRepositoryContextDTO.url())
                .filter(h -> h instanceof RemoteAasHandler<?, ?> || h instanceof AasRepositoryHandler<?, ?>)
                .orElseThrow(() -> new NotFoundException("An AAS Repository with url %s was not found."))
                .patchPolicyBindings(remoteAasRepositoryContextDTO.policyBindings());

        return Response.noContent().build();
    }


    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     *
     * @param uri The URL of the AAS client (as URI)
     */
    @DELETE
    @Path("repository")
    @Override
    public void unregister(@QueryParam("url") URI uri) {
        AasHandler<?, ?> handlerMaybe = aasServerStore.remove(uri);

        var handler = Optional.ofNullable(handlerMaybe)
                .orElseThrow(() -> new NotFoundException(String.format(NOT_FOUND_TEMPLATE, uri)));

        handler.cleanUp();

        if (handler instanceof EventDrivenRepositoryHandler) {
            aasRepositoryManager.stopRepository(uri);
        }
        else if (handler instanceof RemoteHandler remoteHandler) {
            scheduler.removeRunnable(remoteHandler);
        }
    }


    /**
     * Register an AAS service (e.g., FA³ST) to this extension.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     * @return the URI of the registered remote AAS repository.
     * @deprecated Consolidated endpoints to /repository/{local/remote}. Use POST /repository/remote
     */
    @POST
    @Path("service")
    @Deprecated(since = "2.3.0")
    public URI registerService(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        register(remoteAasRepositoryContextDTO);
        return remoteAasRepositoryContextDTO.url();
    }


    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension.
     *
     * @param request Request DTO containing environment path, port and config path
     * @return the URI of the started local FA³ST repository.
     * @deprecated Consolidated endpoints to /repository/{local/remote}. Use POST /repository/local
     */
    @POST
    @Path("environment")
    @Deprecated(since = "2.3.0")
    public URI start(LocalRepositoryDTO request) {
        monitor.warning(String.format("/environment is deprecated, please use POST %s", LOCAL_REPOSITORY_PATH));
        return register(request).aasRepositoryUrl();
    }


    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension.
     *
     * @param repositoryUri the URI of the AAS service to unregister.
     * @deprecated Renamed endpoint to /repository. Use DELETE /repository
     */
    @DELETE
    @Path("service")
    @Deprecated(since = "2.3.0")
    public void unregisterService(@QueryParam("url") URI repositoryUri) {
        unregister(repositoryUri);
    }
}
