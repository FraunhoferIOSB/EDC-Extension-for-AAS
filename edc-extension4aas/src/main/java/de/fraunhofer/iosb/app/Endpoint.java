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
package de.fraunhofer.iosb.app;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Objects;

import static de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository.SelfDescriptionSourceType;

/**
 * Delegates requests to controllers.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class Endpoint {

    private static final String SERVICE_PATH = "service";
    private static final String REGISTRY_PATH = "registry";
    private static final String ENVIRONMENT_PATH = "environment";

    private final Monitor monitor;
    private final AasController aasController;
    private final SelfDescriptionRepository selfDescriptionRepository;

    /**
     * Class constructor
     *
     * @param selfDescriptionRepository Manage self descriptions
     * @param aasController             Communication with AAS services
     * @param monitor                   Logs
     */
    public Endpoint(SelfDescriptionRepository selfDescriptionRepository, AasController aasController, Monitor monitor) {
        this.monitor = monitor;
        this.selfDescriptionRepository = Objects.requireNonNull(selfDescriptionRepository);
        this.aasController = Objects.requireNonNull(aasController);
    }

    /**
     * Register an AAS registry to this extension
     *
     * @param registryUrl The URL of the AAS registry
     * @return Appropriate response regarding input format & current state
     */
    @POST
    @Path(REGISTRY_PATH)
    public Response createRegistry(@QueryParam("url") URL registryUrl) {
        return createEntity(registryUrl, SelfDescriptionSourceType.REGISTRY);
    }

    /**
     * Register an AAS service (e.g., FA³ST) to this extension
     *
     * @param serviceUrl The URL of the AAS client
     * @return Appropriate response regarding input format & current state
     */
    @POST
    @Path(SERVICE_PATH)
    public Response createService(@QueryParam("url") URL serviceUrl) {
        return createEntity(serviceUrl, SelfDescriptionSourceType.SERVICE);
    }

    /**
     * Unregister an AAS registry from this extension
     *
     * @param registryUrl The URL of the registry
     * @return Response "ok" containing status message
     */
    @DELETE
    @Path(REGISTRY_PATH)
    public Response removeRegistry(@QueryParam("url") URL registryUrl) {
        return removeEntity(registryUrl, SelfDescriptionSourceType.REGISTRY);
    }

    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     *
     * @param serviceUrl The URL of the AAS client
     * @return Response "ok" containing status message
     */
    @DELETE
    @Path(SERVICE_PATH)
    public Response removeService(@QueryParam("url") URL serviceUrl) {
        return removeEntity(serviceUrl, SelfDescriptionSourceType.SERVICE);
    }


    /**
     * Create a new AAS service. Either (http) port or AAS config path must be given
     * to ensure communication with the AAS service.
     *
     * @param pathToEnvironment                    Path to new AAS environment
     * @param port                                 Port of service to be created
     * @param pathToAssetAdministrationShellConfig Path of AAS configuration file
     * @return Response containing new AAS URL or error code
     */
    @POST
    @Path(ENVIRONMENT_PATH)
    public Response postAasEnvironment(@QueryParam("environment") String pathToEnvironment,
                                       @QueryParam("config") String pathToAssetAdministrationShellConfig,
                                       @QueryParam("port") String port) {
        monitor.info("Received an environment POST request");
        if (Objects.isNull(pathToEnvironment)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing query parameter 'environment'").build();
        } else if (!port.matches("\\d+")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Query parameter 'port' must be a valid port " +
                    "number").build();
        }

        URL newAssetAdministrationShellUrl;
        try {
            var environmentPath = java.nio.file.Path.of(pathToEnvironment);
            var aasConfigPath = Objects.isNull(pathToAssetAdministrationShellConfig) ?
                    null : java.nio.file.Path.of(pathToAssetAdministrationShellConfig);

            newAssetAdministrationShellUrl = aasController.startService(environmentPath, Integer.parseInt(port),
                    aasConfigPath);

        } catch (InvalidPathException invalidPathException) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Could not resolve path %s".formatted(invalidPathException.getInput()))
                    .build();
        } catch (IOException | EdcException aasServiceException) {
            monitor.severe("Could not start AAS service.", aasServiceException);
            return Response.serverError().entity("Could not start AAS service. Check logs for details").build();
        }

        try (var creationResponse = createEntity(newAssetAdministrationShellUrl, SelfDescriptionSourceType.SERVICE)) {
            if (creationResponse.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
                return Response.status(Status.CREATED).entity(newAssetAdministrationShellUrl).build();
            } else {
                aasController.stopService(newAssetAdministrationShellUrl);
                return creationResponse;
            }
        }
    }


    private Response createEntity(URL url, SelfDescriptionSourceType type) {
        monitor.info("Received a %s POST request".formatted(type));

        if (Objects.isNull(url)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }
        if (Objects.nonNull(selfDescriptionRepository.getSelfDescriptionAsset(url.toString()))) {
            return Response.status(Status.CONFLICT).entity("AAS %s with this URL is already registered.".formatted(type)).build();
        }

        selfDescriptionRepository.createSelfDescription(url, type);
        return Response.status(Status.CREATED).entity("Registered new AAS %s at EDC".formatted(type)).build();
    }

    private Response removeEntity(URL url, SelfDescriptionSourceType type) {
        monitor.info("Received a %s DELETE request".formatted(type));
        if (Objects.isNull(url)) {
            return Response.status(Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }

        if (Objects.isNull(selfDescriptionRepository.getSelfDescriptionAsset(url.toString()))) {
            return Response.status(Status.NOT_FOUND).entity("AAS %s was not registered to EDC".formatted(type)).build();
        }

        // Stop AAS Service if started internally
        selfDescriptionRepository.removeSelfDescription(url.toString());

        return Response.ok("Removed %s from EDC".formatted(type)).build();
    }
}
