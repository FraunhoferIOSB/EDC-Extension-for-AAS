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

import static java.lang.String.format;

import java.io.IOException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.IdsAssetElement;
import de.fraunhofer.iosb.app.model.aas.util.SubmodelUtil;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;

/**
 * Delegates (HTTP) Requests to controllers.
 */
@Consumes({ MediaType.APPLICATION_JSON, MediaType.WILDCARD })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class Endpoint {

    public static final String SELF_DESCRIPTION_PATH = "selfDescription";

    private static final String AAS_REQUEST_PATH = "aas";
    private static final String CONFIG_PATH = "config";
    private static final String CLIENT_PATH = "client";
    private static final String ENVIRONMENT_PATH = "environment";

    private final ConfigurationController configurationController;
    private final AasController aasController;
    private final ResourceController resourceController;
    private final Logger logger;
    private final ObjectMapper objectMapper;

    private Map<URL, SelfDescription> selfDescriptionRepository;

    /**
     * Class constructor
     * 
     * @param selfDescriptionRepository Manage self descriptions
     * @param aasController             Communication with AAS services
     * @param resourceController        Communication with EDC
     */
    public Endpoint(Map<URL, SelfDescription> selfDescriptionRepository, AasController aasController,
            ResourceController resourceController) {
        Objects.requireNonNull(selfDescriptionRepository);
        Objects.requireNonNull(aasController);
        Objects.requireNonNull(resourceController);

        this.configurationController = new ConfigurationController();
        this.selfDescriptionRepository = selfDescriptionRepository;
        this.resourceController = resourceController;
        this.aasController = aasController;
        this.logger = Logger.getInstance();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Return the current configuration values of this extension.
     * 
     * @return Current configuration values
     */
    @GET
    @Path(CONFIG_PATH)
    public Response getConfig() {
        logger.debug("Received a config GET request");
        return configurationController.handleRequest(RequestType.GET, null);
    }

    /**
     * Update the current configuration.
     * 
     * @param newConfigurationJson New configuration values as JSON string
     * @return Response with status code
     */
    @PUT
    @Path(CONFIG_PATH)
    public Response putConfig(String newConfigurationJson) {
        logger.debug("Received a config PUT request");
        Objects.requireNonNull(newConfigurationJson);
        return configurationController.handleRequest(RequestType.PUT, null, newConfigurationJson);
    }

    /**
     * Register a remote AAS service (e.g., FA³ST) to this extension
     * 
     * @param aasServiceUrl The URL of the new AAS client
     * @return Response
     */
    @POST
    @Path(CLIENT_PATH)
    public Response postAasService(@QueryParam("url") URL aasServiceUrl) {
        logger.log("Received a client POST request");
        Objects.requireNonNull(aasServiceUrl);
        if (selfDescriptionRepository.containsKey(aasServiceUrl)) {
            return Response.ok("Service was already registered at EDC").build();
        }
        try {
            var newEnvironment = aasController.getAasModelWithUrls(aasServiceUrl);
            if (Objects.isNull(newEnvironment)) {
                return Response.status(Response.Status.BAD_REQUEST.getStatusCode(),
                        "Could not reach AAS service API by given URL").build();
            }

            registerAasService(aasServiceUrl, newEnvironment);

            return Response.ok("Registered new client at EDC").build();
        } catch (IOException aasServiceUnreachableIoException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                    "Could not reach AAS service: " + aasServiceUnreachableIoException.getMessage()).build();
        } catch (DeserializationException aasModelDeserializationException) {
            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                            "Could not deserialize AAS model: " + aasModelDeserializationException.getMessage())
                    .build();
        }
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
            @QueryParam(CONFIG_PATH) String pathToAssetAdministrationShellConfig, @QueryParam("port") int port) {
        logger.log("Received an environment POST request");
        Objects.requireNonNull(pathToEnvironment);

        try {
            // Full qualifier because jakarta.ws.rs.Path is used for HTTP endpoints
            java.nio.file.Path aasConfigPath = null;
            final java.nio.file.Path environmentPath = java.nio.file.Path.of(pathToEnvironment);
            if (Objects.nonNull(pathToAssetAdministrationShellConfig)) {
                aasConfigPath = java.nio.file.Path.of(pathToAssetAdministrationShellConfig);
            }
            var newAssetAdministrationShellUrl = aasController.startService(environmentPath, port, aasConfigPath);
            if (Objects.nonNull(newAssetAdministrationShellUrl)) {

                // Fetch CustomAssetAdministrationShellEnvironment from newly created service
                var newEnvironment = aasController.getAasModelWithUrls(newAssetAdministrationShellUrl);

                registerAasService(newAssetAdministrationShellUrl, newEnvironment);

                return Response.ok("Booted up and registered AAS service at EDC." + "\nURL of new AAS service: " +
                        newAssetAdministrationShellUrl).build();
            }

        } catch (IOException aasServiceException) {
            logger.error(format("Could not start/read from AAS service. Message from AAS Service: %s",
                    aasServiceException.getMessage()));
        } catch (InvalidPathException invalidPathException) {
            logger.error("Could not resolve paths", invalidPathException);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (DeserializationException aasModelDeserializationException) {
            logger.error("Could not deserialize AAS model.", aasModelDeserializationException);
        }
        logger.error("Could not boot up or register AAS service");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Could not start AAS service. Check logs for details").build();
    }

    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension
     * 
     * @param aasServiceUrl The URL of the new AAS client
     * @return Response "ok" containing status message
     */
    @DELETE
    @Path(CLIENT_PATH)
    public Response removeAasService(@QueryParam("url") URL aasServiceUrl) {
        logger.log("Received a client DELETE request");
        Objects.requireNonNull(aasServiceUrl);
        var selfDescription = selfDescriptionRepository.get(aasServiceUrl);

        if (!selfDescriptionRepository.containsKey(aasServiceUrl)) {
            return Response.ok("Service was not registered to EDC").build();
        }

        if (Objects.nonNull(selfDescription)) {
            var environment = selfDescription.getEnvironment();
            removeAssetsContracts(getAllElements(environment));
        }

        // Stop AAS Service if started internally
        aasController.stopAssetAdministrationShellService(aasServiceUrl);
        selfDescriptionRepository.remove(aasServiceUrl);

        return Response.ok("Unregistered client from EDC").build();
    }

    /**
     * Forward POST request to provided host in requestUrl. If requestUrl is an AAS
     * service that is registered at this EDC, synchronize assets and self
     * description as well.
     * 
     * @param requestUrl  URL of AAS service to be updated
     * @param requestBody AAS element
     * @return Response status
     */
    @POST
    @Path(AAS_REQUEST_PATH)
    public Response postAasRequest(@QueryParam("requestUrl") URL requestUrl, String requestBody) {
        logger.log("Received an AAS POST request");
        Objects.requireNonNull(requestUrl);
        Objects.requireNonNull(requestBody);
        return handleAasRequest(RequestType.POST, requestUrl, new String());
    }

    /**
     * Forward DELETE request to provided host in requestUrl. If requestUrl is an
     * AAS service that is registered at this EDC, synchronize assets and self
     * description aswell.
     * 
     * @param requestUrl URL of AAS service to be deleted
     * @return Response status
     */
    @DELETE
    @Path(AAS_REQUEST_PATH)
    public Response deleteAasRequest(@QueryParam("requestUrl") URL requestUrl) {
        logger.log("Received an AAS DELETE request");
        Objects.requireNonNull(requestUrl);
        return handleAasRequest(RequestType.DELETE, requestUrl, new String());
    }

    /**
     * Forward PUT request to provided host in requestUrl.
     * 
     * @param requestUrl  URL of AAS service to be updated
     * @param requestBody AAS element
     * @return Response status
     */
    @PUT
    @Path(AAS_REQUEST_PATH)
    public Response putAasRequest(@QueryParam("requestUrl") URL requestUrl, String requestBody) {
        logger.log("Received an AAS PUT request");
        Objects.requireNonNull(requestUrl);
        Objects.requireNonNull(requestBody);
        return handleAasRequest(RequestType.PUT, requestUrl, new String());
    }

    /**
     * Print self descriptions of AAS environments registered at this EDC. If no
     * query parameter is given, print all self descriptions available.
     * 
     * @param aasServiceUrl Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    @Path(SELF_DESCRIPTION_PATH)
    public Response getSelfDescription(@QueryParam("aasService") URL aasServiceUrl) {
        if (Objects.isNull(aasServiceUrl)) {
            logger.debug("Received a self description GET request");
            // Build JSON object containing all self descriptions
            var selfDescriptions = objectMapper.createArrayNode();
            selfDescriptionRepository.values().stream().filter(selfDescription -> Objects.nonNull(selfDescription))
                    .forEach(selfDescription -> selfDescriptions.add(selfDescription.toJsonNode()));

            return Response.ok(selfDescriptions.toString()).build();
        } else {
            logger.debug(format("Received a self description GET request to %s", aasServiceUrl));
            var selfDescription = selfDescriptionRepository.get(aasServiceUrl);
            if (Objects.nonNull(selfDescription)) {
                return Response.ok(selfDescription.toString()).build();
            } else {
                logger.error(format("Self description with URL %s not found.", aasServiceUrl.toString()));
                return Response.status(Status.NOT_FOUND).build();
            }
        }
    }

    @POST
    @Path("receiveTransfer/{param}")
    public void testReceive(@PathParam("param") String param, String body) {
        // Testing a data transfer to an HTTP endpoint
        logger.log(body);
    }

    /*
     * Does not synchronize assetIndex & selfDescription. This happens periodically
     * via Synchronizer
     */
    private Response handleAasRequest(RequestType requestType, URL requestUrl, String body) {
        var response = aasController.handleRequest(requestType, requestUrl, body);

        if (!response.getStatusInfo().getFamily().equals(Family.SUCCESSFUL)) {
            logger.error("AAS request failed. Response from URL: " + response.getStatusInfo());
            return Response.status(response.getStatus()).build();
        }
        logger.log("AAS request succeeded.");

        return Response.ok().build();
    }

    private void registerAasService(URL newUrl, CustomAssetAdministrationShellEnvironment newEnvironment) {
        selfDescriptionRepository.put(newUrl, null);
    }

    /*
     * Removes any EDC asset and EDC contract off the EDC
     * AssetIndex/ContractDefinitionStore given a list of AAS elements.
     */
    private void removeAssetsContracts(List<? extends IdsAssetElement> elements) {
        elements.forEach(element -> {
            resourceController.deleteAssetAndContracts(element.getIdsAssetId());
            resourceController.deleteContract(element.getIdsContractId());
        });
    }

    /*
     * Returns all AAS elements in a flattened list format.
     */
    private List<? extends IdsAssetElement> getAllElements(CustomAssetAdministrationShellEnvironment env) {
        var allElements = new ArrayList<IdsAssetElement>();
        allElements.addAll(env.getConceptDescriptions());
        allElements.addAll(env.getAssetAdministrationShells());
        allElements.addAll(env.getSubmodels());
        env.getSubmodels().forEach(submodel -> allElements.addAll(SubmodelUtil.getAllSubmodelElements(submodel)));
        return allElements;
    }
}
