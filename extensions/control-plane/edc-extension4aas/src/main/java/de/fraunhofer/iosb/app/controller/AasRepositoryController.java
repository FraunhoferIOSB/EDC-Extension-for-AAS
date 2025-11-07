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

import de.fraunhofer.iosb.app.controller.dto.AasRegistryContextDTO;
import de.fraunhofer.iosb.app.controller.dto.LocalRepositoryDTO;
import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.app.executor.VariableRateScheduler;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.handler.aas.registry.RemoteAasRegistryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.event.EventDrivenRepositoryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.event.impl.LocalFaaastRepositoryHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.period.impl.RemoteAasRepositoryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;
import de.fraunhofer.iosb.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import de.fraunhofer.iosb.repository.AasRepositoryManager;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryConfig;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URI;
import java.util.Optional;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Handles requests regarding the Asset Administration Shells registered to this
 * extension
 */
@Consumes(APPLICATION_JSON)
@Path("/")
public class AasRepositoryController {

    private static final String ENVIRONMENT_PATH = "environment";
    private static final String SERVICE_PATH = "service";
    private static final String NOT_FOUND_TEMPLATE = "%s not found in registered AAS servers.";
    private static final String EXISTS_TEMPLATE = "%s is already a registered server";
    private static final String REGISTRY_PATH = "registry";

    private final AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager;
    private final Monitor monitor;

    private final AasServerStore aasServerStore;

    // Manages remote AAS repositories by polling their API
    private final VariableRateScheduler scheduler;
    private final EdcStoreHandler edcStoreHandler;


    public AasRepositoryController(Monitor monitor, AasServerStore aasServerStore,
                                   AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager,
                                   EdcStoreHandler edcStoreHandler) {
        this.monitor = monitor;
        this.aasServerStore = aasServerStore;
        this.aasRepositoryManager = aasRepositoryManager;
        this.scheduler = new VariableRateScheduler(1, monitor);
        scheduler.run();

        this.edcStoreHandler = edcStoreHandler;
    }

    /**
     * Register an AAS service at the EDC.
     *
     * @param remoteAasRepositoryContextDTO remoteRepositoryDTO
     */
    @POST
    @Path(SERVICE_PATH)
    public URI register(RemoteAasRepositoryContextDTO remoteAasRepositoryContextDTO) {
        if (aasServerStore.isStored(remoteAasRepositoryContextDTO.uri())) {
            throw new WebApplicationException(String.format(EXISTS_TEMPLATE, remoteAasRepositoryContextDTO.uri()), Response.Status.CONFLICT);
        }

        var context = new RemoteAasRepositoryContext.Builder()
                .uri(remoteAasRepositoryContextDTO.uri())
                .policyBindings(remoteAasRepositoryContextDTO.policyBindings())
                .authenticationMethod(remoteAasRepositoryContextDTO.authenticationMethod())
                .build();

        var handler = new RemoteAasRepositoryHandler(monitor, context, edcStoreHandler);

        aasServerStore.put(context.getUri(), handler);
        scheduler.addRunnable(handler);

        return context.getUri();
    }

    /**
     * Register an AAS registry at the EDC.
     *
     * @param aasRegistryContextDTO registry DTO
     */
    @POST
    @Path(REGISTRY_PATH)
    public URI register(AasRegistryContextDTO aasRegistryContextDTO) {
        if (aasServerStore.isStored(aasRegistryContextDTO.uri())) {
            throw new WebApplicationException(String.format(EXISTS_TEMPLATE, aasRegistryContextDTO.uri()), Response.Status.CONFLICT);
        }

        var context = new AasRegistryContext.Builder()
                .uri(aasRegistryContextDTO.uri())
                .authenticationMethod(aasRegistryContextDTO.authenticationMethod())
                .build();

        var handler = new RemoteAasRegistryHandler(monitor, context, edcStoreHandler);

        aasServerStore.put(context.getUri(), handler);
        scheduler.addRunnable(handler);

        return context.getUri();
    }

    /**
     * Starts a FA³ST service internally and registers it to the EDC.
     *
     * @param request Request DTO containing environment path, port and config path
     */
    @POST
    @Path(ENVIRONMENT_PATH)
    public URI start(LocalRepositoryDTO request) {
        monitor.debug(String.format("Starting FA³ST repository with model file at %s.", request.modelPath()));
        FaaastRepositoryConfig config = FaaastRepositoryConfig.Builder.newInstance()
                .model(request.modelPath())
                .port(request.port())
                .configPath(request.configPath())
                .build();

        LocalFaaastRepositoryContext context = aasRepositoryManager.startRepository(config);

        AasRepositoryHandler handler = new LocalFaaastRepositoryHandler(monitor, context, edcStoreHandler);

        aasServerStore.put(context.getUri(), handler);

        return context.getUri();
    }

    /**
     * Register an AAS registry at the EDC.
     *
     * @param registryUri registry to unregister
     */
    @DELETE
    @Path(REGISTRY_PATH)
    public void unregisterRegistry(@QueryParam("url") URI registryUri) {
        unregister(registryUri);
    }


    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     *
     * @param repositoryUri The URL of the AAS client (as URI)
     */
    @DELETE
    @Path(SERVICE_PATH)
    public void unregisterRepository(@QueryParam("url") URI repositoryUri) {
        unregister(repositoryUri);
    }

    private void unregister(URI uri) {
        AasHandler handlerMaybe = aasServerStore.remove(uri);

        var handler = Optional.ofNullable(handlerMaybe)
                .orElseThrow(() -> new NotFoundException(String.format(NOT_FOUND_TEMPLATE, uri)));

        handler.cleanUp();

        if (handler instanceof EventDrivenRepositoryHandler) {
            aasRepositoryManager.stopRepository(uri);
        } else if (handler instanceof RemoteHandler remoteHandler) {
            scheduler.removeRunnable(remoteHandler);
        }
    }

    /**
     * Stops all internally started AAS services
     */
    public void stopAll() {
        monitor.debug("Stopping all internally started AAS services...");
        aasServerStore.keySet().forEach(this::unregister);
        scheduler.terminate();
    }
}
