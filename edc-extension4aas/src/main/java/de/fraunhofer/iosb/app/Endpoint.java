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
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionSerializer;
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
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Delegates (HTTP) Requests to controllers.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
@Produces({MediaType.APPLICATION_JSON})
@Path("/")
public class Endpoint {

    public static final String SELF_DESCRIPTION_PATH = "selfDescription";
    private static final String SERVICE_PATH = "service";
    private static final String ENVIRONMENT_PATH = "environment";

    private final Monitor monitor;
    private final AasController aasController;
    private final SelfDescriptionRepository selfDescriptionRepository;

    /**
     * Class constructor
     *
     * @param selfDescriptionRepository Manage self descriptions
     * @param aasController             Communication with AAS services
     */
    public Endpoint(SelfDescriptionRepository selfDescriptionRepository, AasController aasController, Monitor monitor) {
        this.monitor = monitor;
        this.selfDescriptionRepository = Objects.requireNonNull(selfDescriptionRepository);
        this.aasController = Objects.requireNonNull(aasController);
    }

    /**
     * Register a remote AAS service (e.g., FA³ST) to this extension
     *
     * @param aasServiceUrl The URL of the new AAS client
     * @return Response
     */
    @POST
    @Path(SERVICE_PATH)
    public Response postAasService(@QueryParam("url") URL aasServiceUrl) {
        monitor.info("Received a client POST request");
        if (Objects.isNull(aasServiceUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }
        if (Objects.nonNull(selfDescriptionRepository.getSelfDescription(aasServiceUrl))) {
            return Response.status(Status.CONFLICT).entity("A service with this URL is already registered.").build();
        }
        selfDescriptionRepository.createSelfDescription(aasServiceUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE);
        return Response.ok("Registered new client at EDC").build();
    }

    /**
     * Create a new AAS service. Either (http) port or AAS config path must be given
     * to ensure communication with the AAS service.
     *
     * @param pathToEnvironment                    Path to new AAS environment
     *                                             (required)
     * @param port                                 Port of service to be created
     *                                             (optional)
     * @param pathToAssetAdministrationShellConfig Path of AAS configuration file
     *                                             (optional)
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
            return Response.serverError().entity("Could not start AAS service. Check connector logs for details").build();
        }

        selfDescriptionRepository.createSelfDescription(newAssetAdministrationShellUrl,
                SelfDescriptionRepository.SelfDescriptionSourceType.SERVICE);
        return Response.status(Status.CREATED).entity(newAssetAdministrationShellUrl).build();
    }

    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     *
     * @param aasServiceUrl The URL of the new AAS client
     * @return Response "ok" containing status message
     */
    @DELETE
    @Path(SERVICE_PATH)
    public Response removeAasService(@QueryParam("url") URL aasServiceUrl) {
        monitor.info("Received a client DELETE request");
        if (Objects.isNull(aasServiceUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }

        if (Objects.isNull(selfDescriptionRepository.getSelfDescription(aasServiceUrl))) {
            return Response.status(Status.NOT_FOUND).entity("Service was not registered to EDC").build();
        }

        // Stop AAS Service if started internally
        aasController.stopAssetAdministrationShellService(aasServiceUrl);
        selfDescriptionRepository.removeSelfDescription(aasServiceUrl);

        return Response.ok("Unregistered client from EDC").build();
    }

    /**
     * Print self-descriptions of AAS environments registered at this EDC. If no
     * query parameter is given, print all self-descriptions available.
     *
     * @param aasServiceUrl Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    @Path(SELF_DESCRIPTION_PATH)
    public Response getSelfDescription(@QueryParam("aasService") URL aasServiceUrl) {
        if (Objects.isNull(aasServiceUrl)) {
            monitor.debug("Received a self description GET request");

            var sdArrayNode = selfDescriptionRepository.getAllSelfDescriptions().values()
                    .stream().filter(Objects::nonNull).map(SelfDescriptionSerializer::assetToString).collect(Collectors.joining(","));

            return Response.ok(sdArrayNode).build();
        } else {
            monitor.debug("Received a self description GET request for %s".formatted(aasServiceUrl));
            var selfDescriptionAsset = selfDescriptionRepository.getSelfDescription(aasServiceUrl);
            if (Objects.nonNull(selfDescriptionAsset)) {
                return Response.ok(SelfDescriptionSerializer.assetToString(selfDescriptionAsset)).build();
            } else {
                monitor.warning(format("Self description with URL %s not found.", aasServiceUrl));
                return Response.status(Status.NOT_FOUND).build();
            }
        }
    }

}
