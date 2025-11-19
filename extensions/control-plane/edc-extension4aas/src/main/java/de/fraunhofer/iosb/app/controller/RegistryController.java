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
import de.fraunhofer.iosb.app.executor.VariableRateScheduler;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.app.handler.aas.registry.RemoteAasRegistryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.registry.AasRegistryClient;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.net.URI;
import java.util.Optional;

import static de.fraunhofer.iosb.app.controller.RegistryController.REGISTRY_PATH;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;


/**
 * Handles requests regarding the AAS registries registered to this extension
 */
@Consumes(APPLICATION_JSON)
@Path(REGISTRY_PATH)
public class RegistryController extends AbstractAasServerController {
    public static final String REGISTRY_PATH = "registry";


    public RegistryController(Monitor monitor, AasServerStore aasServerStore, EdcStoreHandler edcStoreHandler) {
        super(monitor, aasServerStore, new VariableRateScheduler(1, monitor), edcStoreHandler);
    }


    /**
     * Register an AAS registry at the EDC.
     *
     * @param aasRegistryContextDTO registry DTO
     */
    @POST
    public URI register(AasRegistryContextDTO aasRegistryContextDTO) {
        monitor.debug(String.format("Registering AAS registry with URL %s.", aasRegistryContextDTO.url()));
        if (aasServerStore.isStored(aasRegistryContextDTO.url())) {
            throw new WebApplicationException(String.format(EXISTS_TEMPLATE, aasRegistryContextDTO.url()), Response.Status.CONFLICT);
        }

        AasRegistryContext context = aasRegistryContextDTO.asContext();
        AasRegistryClient client = new AasRegistryClient(context);

        RemoteAasRegistryHandler handler;
        try {
            handler = new RemoteAasRegistryHandler(monitor, client, edcStoreHandler);
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
     * Register an AAS registry at the EDC.
     *
     * @param registryUri registry to unregister
     */
    @DELETE
    @Override
    public void unregister(@QueryParam("url") URI registryUri) {
        AasHandler<?> handlerMaybe = aasServerStore.remove(registryUri);

        var handler = Optional.ofNullable(handlerMaybe)
                .orElseThrow(() -> new NotFoundException(String.format(NOT_FOUND_TEMPLATE, registryUri)));

        handler.cleanUp();

        if (handler instanceof RemoteHandler remoteHandler) {
            scheduler.removeRunnable(remoteHandler);
        }
    }
}
