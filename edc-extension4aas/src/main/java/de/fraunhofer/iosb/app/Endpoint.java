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
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javax.annotation.Nullable;

import static de.fraunhofer.iosb.app.model.aas.service.ServiceRepository.SelfDescriptionSourceType;
import static de.fraunhofer.iosb.app.model.aas.service.ServiceRepository.SelfDescriptionSourceType.REGISTRY;
import static de.fraunhofer.iosb.app.model.aas.service.ServiceRepository.SelfDescriptionSourceType.SERVICE;

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
    private final RegistryRepository registryRepository;
    private final ServiceRepository serviceRepository;

    /**
     * Class constructor
     *
     * @param serviceRepository  Manage AAS services
     * @param registryRepository Manage AAS registries
     * @param aasController      Communication with AAS services
     * @param monitor            Logs
     */
    public Endpoint(ServiceRepository serviceRepository, RegistryRepository registryRepository,
                    AasController aasController, Monitor monitor) {
        this.monitor = Objects.requireNonNullElseGet(monitor, ConsoleMonitor::new);
        this.serviceRepository = Objects.requireNonNull(serviceRepository);
        this.registryRepository = Objects.requireNonNull(registryRepository);
        this.aasController = Objects.requireNonNull(aasController);
    }

    /**
     * Register an AAS registry to this extension
     *
     * @param registryUrl The URL of the AAS registry
     * @return Status message about the success of this operation.
     */
    @POST
    @Path(REGISTRY_PATH)
    public Response createRegistry(@QueryParam("url") URL registryUrl) {
        monitor.info("POST /%s".formatted(REGISTRY_PATH));
        return createEntity(registryUrl, REGISTRY);
    }

    /**
     * Register an AAS service to this extension
     *
     * @param serviceUrl The URL of the AAS service
     * @return Status message about the success of this operation.
     */
    @POST
    @Path(SERVICE_PATH)
    public Response createService(@QueryParam("url") URL serviceUrl) {
        monitor.info("POST /%s".formatted(SERVICE_PATH));
        return createEntity(serviceUrl, SERVICE);
    }

    /**
     * Unregister an AAS registry from this extension
     *
     * @param registryUrl The URL of the registry
     * @return Status message about the success of this operation.
     */
    @DELETE
    @Path(REGISTRY_PATH)
    public Response removeRegistry(@QueryParam("url") URL registryUrl) {
        monitor.info("DELETE /%s".formatted(REGISTRY_PATH));
        return removeEntity(registryUrl, REGISTRY);
    }

    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     *
     * @param serviceUrl The URL of the AAS client
     * @return Status message about the success of this operation.
     */
    @DELETE
    @Path(SERVICE_PATH)
    public Response removeService(@QueryParam("url") URL serviceUrl) {
        monitor.info("DELETE /%s".formatted(SERVICE_PATH));
        return removeEntity(serviceUrl, SERVICE);
    }

    /**
     * Create a new AAS service. Either (http) port or AAS config path must be given
     * to ensure communication with the AAS service.
     *
     * @param pathToEnvironment Path to new AAS environment
     * @param port              Port of service to be created
     * @param pathToConfig      Path of AAS configuration file
     * @return Response containing new AAS URL or error code
     */
    @POST
    @Path(ENVIRONMENT_PATH)
    public Response postAasEnvironment(@QueryParam("environment") String pathToEnvironment,
                                       @QueryParam("config") String pathToConfig,
                                       @QueryParam("port") int port) {
        monitor.info("POST /%s".formatted(ENVIRONMENT_PATH));
        if (Objects.isNull(pathToEnvironment)) {
            return Response.status(Status.BAD_REQUEST).entity("Missing query parameter 'environment'").build();
        }

        var environmentPath = fromString(pathToEnvironment);
        var aasConfigPath = fromString(pathToConfig);

        URL serviceAccessUrl;
        try {
            serviceAccessUrl = aasController.startService(environmentPath, port, aasConfigPath);
        } catch (IllegalArgumentException illegalArgumentException) {
            return Response.status(Status.BAD_REQUEST).entity(illegalArgumentException.getMessage()).build();
        } catch (IOException | EdcException aasServiceException) {
            monitor.severe("Could not start AAS service.", aasServiceException);
            return Response.serverError().entity("Could not start AAS service. Check logs for details").build();
        }

        // From here, do the same as if it were a remote service.
        try (var creationResponse = createEntity(serviceAccessUrl, SERVICE)) {
            if (creationResponse.getStatusInfo().getFamily().equals(Status.Family.SUCCESSFUL)) {
                return Response.status(Status.CREATED).entity(serviceAccessUrl).build();
            } else {
                aasController.stopService(serviceAccessUrl);
                return creationResponse;
            }
        }
    }

    private @Nullable java.nio.file.Path fromString(String pathAsString) {
        if (pathAsString == null) {
            return null;
        }
        return java.nio.file.Path.of(pathAsString);
    }

    private Response createEntity(URL accessUrl, SelfDescriptionSourceType type) {
        if (Objects.isNull(accessUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }

        if (SERVICE.equals(type) && serviceRepository.create(accessUrl) ||
                REGISTRY.equals(type) && registryRepository.create(accessUrl)) {
            return Response.status(Status.CREATED).entity("Registered new AAS %s at EDC".formatted(type)).build();
        }

        return Response.status(Status.CONFLICT)
                .entity("AAS %s with this URL is already registered."
                        .formatted(type.toString().toLowerCase()))
                .build();
    }

    private Response removeEntity(URL accessUrl, SelfDescriptionSourceType type) {
        if (Objects.isNull(accessUrl)) {
            return Response.status(Status.BAD_REQUEST).entity("Missing query parameter 'url'").build();
        }

        if (SERVICE.equals(type) && serviceRepository.delete(accessUrl) ||
                REGISTRY.equals(type) && registryRepository.delete(accessUrl)) {
            // Return 204 (https://www.rfc-editor.org/rfc/rfc9110.html#section-9.3.5)
            return Response.noContent().build();
        } else {
            return Response.status(Status.NOT_FOUND).build();
        }
    }
}
