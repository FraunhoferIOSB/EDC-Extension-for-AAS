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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.controller.dto.LocalRepositoryDTO;
import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.app.executor.VariableRateScheduler;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.event.EventDrivenRepositoryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.event.impl.LocalFaaastRepositoryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.period.impl.RemoteAasRepositoryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.local.impl.LocalFaaastRepositoryClient;
import de.fraunhofer.iosb.client.repository.remote.impl.RemoteAasRepositoryClient;
import de.fraunhofer.iosb.model.config.impl.faaast.FaaastRepositoryConfig;
import de.fraunhofer.iosb.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import de.fraunhofer.iosb.repository.AasRepositoryManager;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;

import java.net.ConnectException;
import java.net.URI;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Handles requests regarding the Asset Administration Shells registered to this extension
 */
@Consumes(APPLICATION_JSON)
@Path("/")
public class RepositoryController extends AbstractAasServerController {

    private static final String LOCAL_REPOSITORY_PATH = "repository/local";
    private static final String REMOTE_REPOSITORY_PATH = "repository/remote";

    private final AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager;


    public RepositoryController(Monitor monitor, AasServerStore aasServerStore,
                                Hostname hostname,
                                EdcStoreHandler edcStoreHandler) {
        super(monitor, aasServerStore, new VariableRateScheduler(1, monitor), edcStoreHandler);
        // Use FA³ST to start repositories internally.
        this.aasRepositoryManager = new FaaastRepositoryManager(monitor, hostname);
    }


    /**
     * Starts a FA³ST service internally and registers it to the EDC.
     *
     * @param localRepositoryDTO Request DTO containing environment path, port and config path
     * @throws WebApplicationException malformed request
     */
    @POST
    @Path(LOCAL_REPOSITORY_PATH)
    public URI register(LocalRepositoryDTO localRepositoryDTO) {
        monitor.debug(String.format("Starting FA³ST repository with model file at %s.", localRepositoryDTO.modelPath()));
        FaaastRepositoryConfig config = localRepositoryDTO.asConfig();

        LocalFaaastRepositoryContext context = aasRepositoryManager.startRepository(config);
        LocalFaaastRepositoryClient client = new LocalFaaastRepositoryClient(context);

        LocalFaaastRepositoryHandler handler;
        try {
            handler = new LocalFaaastRepositoryHandler(monitor, client, edcStoreHandler);
        }
        catch (ConnectException connectException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, client.getUri()), connectException);
            throw new WebApplicationException(badRequest(CONNECT_EXCEPTION_TEMPLATE, client.getUri()));
        }
        catch (UnauthorizedException unauthorizedException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, client.getUri()), unauthorizedException);
            throw new WebApplicationException(badRequest(UNAUTHORIZED_EXCEPTION_TEMPLATE, client.getUri()));
        }

        aasServerStore.put(context.getUri(), handler);

        return context.getUri();

    }


    /**
     * Register an AAS service at the EDC.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     * @throws WebApplicationException malformed request
     */
    @POST
    @Path(REMOTE_REPOSITORY_PATH)
    public URI register(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        monitor.debug(String.format("Registering AAS repository with URL %s.", remoteAasRepositoryContextDTO.url()));
        if (aasServerStore.isStored(remoteAasRepositoryContextDTO.url())) {
            throw new WebApplicationException(String.format(EXISTS_TEMPLATE, remoteAasRepositoryContextDTO.url()), Response.Status.CONFLICT);
        }

        RemoteAasRepositoryContext context = remoteAasRepositoryContextDTO.asContext();
        RemoteAasRepositoryClient client = new RemoteAasRepositoryClient(context);

        RemoteAasRepositoryHandler handler;
        try {
            handler = new RemoteAasRepositoryHandler(monitor, client, edcStoreHandler);
        }
        catch (ConnectException connectException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, client.getUri()), connectException);
            throw new WebApplicationException(badRequest(CONNECT_EXCEPTION_TEMPLATE, client.getUri()));
        }
        catch (UnauthorizedException unauthorizedException) {
            monitor.warning(String.format(CONNECT_EXCEPTION_TEMPLATE, client.getUri()), unauthorizedException);
            throw new WebApplicationException(badRequest(UNAUTHORIZED_EXCEPTION_TEMPLATE, client.getUri()));
        }

        aasServerStore.put(context.getUri(), handler);
        scheduler.addRunnable(handler);

        return context.getUri();
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
        AasHandler<?> handlerMaybe = aasServerStore.remove(uri);

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
     * @deprecated Consolidated endpoints to /repository/{local/remote}. Use POST /repository/remote
     */
    @POST
    @Path("service")
    @Deprecated(since = "2.3.0")
    public URI registerService(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        return register(remoteAasRepositoryContextDTO);
    }


    /**
     * @deprecated Consolidated endpoints to /repository/{local/remote}. Use POST /repository/local
     */
    @POST
    @Path("environment")
    @Deprecated(since = "2.3.0")
    public URI start(LocalRepositoryDTO request) {
        monitor.warning(String.format("/environment is deprecated, please use POST %s", LOCAL_REPOSITORY_PATH));
        return register(request);
    }


    /**
     * @deprecated Renamed endpoint to /repository. Use DELETE /repository
     */
    @DELETE
    @Path("service")
    @Deprecated(since = "2.3.0")
    public void unregisterService(@QueryParam("url") URI repositoryUri) {
        unregister(repositoryUri);
    }
}
