package de.fraunhofer.iosb.app;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.IdsAssetElement;
import de.fraunhofer.iosb.app.model.aas.util.SubmodelUtil;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
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
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Delegates (HTTP) Requests to controllers.
 */
@Consumes({ MediaType.APPLICATION_JSON, MediaType.WILDCARD })
@Produces({ MediaType.APPLICATION_JSON })
@Path("/")
public class Endpoint {

    private static final String AAS_REQUEST_PATH = "aas";
    private final Configuration config = Configuration.getInstance();

    private final ConfigurationController configurationController;
    private final AasController aasController;
    private final ResourceController resourceController;
    private final Logger logger;
    private final ObjectMapper objectMapper;

    private SelfDescriptionRepository selfDescriptionRepository;

    /**
     * Class constructor

     * @param selfDescriptionRepository Manage self descriptions
     * @param aasController             Communication with AAS services
     * @param resourceController        Communication with EDC
     */
    public Endpoint(SelfDescriptionRepository selfDescriptionRepository, AasController aasController,
            ResourceController resourceController) {
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

     * @return Current configuration values
     */
    @GET
    @Path("config")
    public Response getConfig() {
        logger.debug("Received a config GET request");
        return configurationController.handleRequest(RequestType.GET, null);
    }

    /**
     * Update the current configuration.

     * @param newConfigurationJson New configuration values as JSON string
     * @return Response with status code
     */
    @PUT
    @Path("config")
    public Response putConfig(String newConfigurationJson) {
        logger.debug("Received a config PUT request");
        return configurationController.handleRequest(RequestType.PUT, null, newConfigurationJson);
    }

    /**
     * Register a remote AAS service (e.g., FA³ST) to this extension

     * @param aasServiceUrl The URL of the new AAS client
     * @return Response
     */
    @POST
    @Path("client")
    public Response postAasService(@QueryParam("url") URL aasServiceUrl) {
        logger.log("Received a client POST request");
        Objects.requireNonNull(aasServiceUrl);
        if (Objects.nonNull(selfDescriptionRepository.get(aasServiceUrl))) {
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

     * @param pathToEnvironment                    Path to new AAS environment
     *                                             (required)
     * @param port                                 Port of service to be created
     *                                             (optional)
     * @param pathToAssetAdministrationShellConfig Path of AAS configuration file
     *                                             (optional)
     * @return Response containing new AAS URL or error code
     */
    @POST
    @Path("environment")
    public Response postAasEnvironment(@QueryParam("environment") String pathToEnvironment,
            @QueryParam("config") String pathToAssetAdministrationShellConfig, @QueryParam("port") int port) {
        logger.log("Received an environment POST request");
        Objects.requireNonNull(pathToEnvironment);

        try {
            // Full qualifier because jakarta.ws.rs.Path is used for HTTP endpoints
            java.nio.file.Path aasConfigPath = null;
            final java.nio.file.Path environmentPath = java.nio.file.Path.of(pathToEnvironment);
            if (Objects.nonNull(pathToAssetAdministrationShellConfig)) {
                aasConfigPath = java.nio.file.Path.of(pathToAssetAdministrationShellConfig);
            }
            final var newAssetAdministrationShellUrl = aasController.startService(environmentPath, port, aasConfigPath);
            if (Objects.nonNull(newAssetAdministrationShellUrl)) {

                // Fetch CustomAssetAdministrationShellEnvironment from newly created service
                final var newEnvironment = aasController.getAasModelWithUrls(newAssetAdministrationShellUrl);

                registerAasService(newAssetAdministrationShellUrl, newEnvironment);

                return Response.ok("Booted up and registered AAS service at EDC." + "\nURL of new AAS service: " +
                        newAssetAdministrationShellUrl).build();
            }

        } catch (IOException aasServiceException) {
            logger.error("Could not start/read from AAS service. Message from AAS Service: " +
                    aasServiceException.getMessage());
        } catch (java.nio.file.InvalidPathException invalidPathException) {
            logger.error("Could not resolve paths", invalidPathException);
            return Response.status(Response.Status.BAD_REQUEST).build();
        } catch (DeserializationException aasModelDeserializationException) {
            logger.error("Could not deserialize AAS model: ", aasModelDeserializationException);
        }
        logger.error("Could not boot up or register AAS service");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Could not start AAS service.Check logs for details").build();
    }

    /**
     * Unregister an AAS service (e.g., FA³ST) from this extension

     * @param aasServiceUrl The URL of the new AAS client
     * @return Response "ok" containing status message
     */
    @DELETE
    @Path("client")
    public Response removeAasService(@QueryParam("url") URL aasServiceUrl) {
        logger.log("Received a client DELETE request");
        Objects.requireNonNull(aasServiceUrl);
        final var selfDescription = selfDescriptionRepository.get(aasServiceUrl);

        if (Objects.isNull(selfDescription)) {
            return Response.ok("Service was not registered to EDC").build();
        }

        final var environment = selfDescription.getEnvironment();

        removeAssetsContracts(environment);

        // Stop AAS Service if started internally
        aasController.stopAssetAdministrationShellService(aasServiceUrl);
        selfDescriptionRepository.remove(aasServiceUrl);

        return Response.ok("Unregistered client from EDC").build();
    }

    /**
     * Forward POST request to provided host in requestUrl. If requestUrl is an AAS
     * service that is registered at this EDC, synchronize assets and self
     * description as well.

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
        final var response = aasController.handleRequest(RequestType.POST, requestUrl, "", requestBody);
        if (response.getStatusInfo() != Response.Status.CREATED) {
            logger.error("AAS request failed. Response from URL: " + response.getStatusInfo());
            return Response.status(response.getStatus()).build();
        }
        URL requestUrlNoPath;
        try {
            requestUrlNoPath = new URL(requestUrl.toString().replace(requestUrl.getPath(), ""));
            syncAasWithEdc(requestUrlNoPath);
        } catch (MalformedURLException e) {
            logger.error("Could not determine AAS service from URL", e);
            return Response.serverError().build();
        } catch (EdcException edcException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), edcException.getMessage())
                    .build();
        }

        logger.log("AAS request succeeded.");

        return Response.ok().build();
    }

    /**
     * Forward DELETE request to provided host in requestUrl. If requestUrl is an
     * AAS service that is registered at this EDC, synchronize assets and self
     * description aswell.

     * @param requestUrl URL of AAS service to be deleted
     * @return Response status
     */
    @DELETE
    @Path(AAS_REQUEST_PATH)
    public Response deleteAasRequest(@QueryParam("requestUrl") URL requestUrl) {
        logger.log("Received an AAS DELETE request");
        Objects.requireNonNull(requestUrl);
        final var response = aasController.handleRequest(RequestType.DELETE, requestUrl, "", null);
        if (response.getStatusInfo() != Response.Status.CREATED) {
            logger.error("AAS request failed. Response from URL: " + response.getStatusInfo());
            return Response.status(response.getStatus()).build();
        }
        URL requestUrlNoPath;
        try {
            requestUrlNoPath = new URL(requestUrl.toString().replace(requestUrl.getPath(), ""));
            syncAasWithEdc(requestUrlNoPath);
        } catch (MalformedURLException e) {
            logger.error("Could not determine AAS service from URL", e);
            return Response.serverError().build();
        } catch (EdcException edcException) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), edcException.getMessage())
                    .build();
        }

        logger.log("AAS request succeeded.");

        return Response.ok().build();
    }

    /**
     * Forward PUT request to provided host in requestUrl.

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
        return aasController.handleRequest(RequestType.PUT, requestUrl, "", requestBody);
    }

    /**
     * Print self descriptions of AAS environments registered at this EDC. If no
     * query parameter is given, print all self descriptions available.

     * @param aasServiceUrl Specify an AAS environment by its service
     * @return Self description(s)
     */
    @GET
    @Path("selfDescription")
    public Response getSelfDescription(@QueryParam("aasService") URL aasServiceUrl) {
        logger.debug("Received a self description GET request");
        if (!config.isExposeSelfDescription()) {
            logger.log("Self description will not be exposed due to configuration");
            return Response.status(Status.FORBIDDEN).build();
        }

        if (Objects.isNull(aasServiceUrl)) { // get all
            // Build large JSON object containing all the self descriptions
            var selfDescriptionStringBuilder = new StringBuilder().append("[")
                    .append(selfDescriptionRepository.getAll().values().stream()
                            .map(selfDescription -> selfDescription.toString()).collect(Collectors.joining(",")))
                    .append("]");
            return Response.ok(selfDescriptionStringBuilder.toString()).build();
        } else {
            logger.debug("Received a self description GET request to " + aasServiceUrl);
            var selfDescription = selfDescriptionRepository.get(aasServiceUrl);
            if (Objects.nonNull(selfDescription)) {
                return Response.ok(selfDescription.toString()).build();
            } else {
                logger.error("Self description with URL " + aasServiceUrl.toString() + " not found.");
                return Response.status(Status.NOT_FOUND).build();
            }
        }
    }

    @POST
    @Path("receiveTransfer/{param}")
    public void testReceive(@PathParam("param") String param, String body) {
        logger.log(body);
    }

    /**
     * Synchronize AAS element structure w/ EDC AssetIndex

     * @param aasUrl AAS Service URL
     */
    public void syncAasWithEdc(URL aasUrl) {
        final var oldSelfDescription = selfDescriptionRepository.get(aasUrl);
        CustomAssetAdministrationShellEnvironment newEnvironment;

        try {
            newEnvironment = aasController.getAasModelWithUrls(aasUrl);
        } catch (IOException aasServiceUnreachableException) {
            throw new EdcException("Could not reach AAS service: " + aasServiceUnreachableException.getMessage());
        } catch (DeserializationException aasModelDeserializationException) {
            throw new EdcException("Could not deserialize AAS model: " + aasModelDeserializationException.getMessage());
        }

        if (Objects.isNull(newEnvironment) || Objects.isNull(oldSelfDescription)) {
            throw new EdcException(
                    "Could not reach AAS service by URL" + aasUrl + "or AAS service is not registered at EDC");
        }

        var oldEnvironment = oldSelfDescription.getEnvironment();

        newEnvironment.getAssetAdministrationShells().replaceAll(
                shell -> oldEnvironment.getAssetAdministrationShells().contains(shell)
                        ? oldEnvironment.getAssetAdministrationShells()
                                .get(oldEnvironment.getAssetAdministrationShells().indexOf(shell))
                        : shell);
        newEnvironment.getSubmodels()
                .replaceAll(shell -> oldEnvironment.getSubmodels().contains(shell)
                        ? oldEnvironment.getSubmodels().get(oldEnvironment.getSubmodels().indexOf(shell))
                        : shell);
        newEnvironment.getConceptDescriptions()
                .replaceAll(shell -> oldEnvironment.getConceptDescriptions().contains(shell)
                        ? oldEnvironment.getConceptDescriptions()
                                .get(oldEnvironment.getConceptDescriptions().indexOf(shell))
                        : shell);

        addAssetsContracts(getAllElements(newEnvironment).stream()
                .filter(element -> Objects.isNull(element.getIdsContractId())).collect(Collectors.toList()));

        var oldElements = getAllElements(oldEnvironment);
        oldElements.removeAll(getAllElements(newEnvironment));
        removeAssetsContracts(oldElements);

        selfDescriptionRepository.update(aasUrl, new SelfDescription(newEnvironment));
    }

    private void registerAasService(final URL newUrl, final CustomAssetAdministrationShellEnvironment newEnvironment) {
        // Create the self description
        // NOTE: Initially, access URLs of the AAS elements are stored in their asset
        // ID field.
        var allElements = getAllElements(newEnvironment);

        addAssetsContracts(allElements);
        selfDescriptionRepository.add(newUrl, new SelfDescription(newEnvironment));
    }

    private void addAssetsContracts(List<? extends IdsAssetElement> elements) {
        // Add each AAS element to EDC assetStore, giving it a contract
        elements.forEach(element -> {
            var sourceUrl = element.getIdsAssetId();

            var assetContractPair = resourceController.createResource(sourceUrl);

            element.setIdsAssetId(assetContractPair.getFirst());
            element.setIdsContractId(assetContractPair.getSecond());
        });
    }

    private void removeAssetsContracts(CustomAssetAdministrationShellEnvironment environment) {
        removeAssetsContracts(getAllElements(environment));
    }

    private void removeAssetsContracts(List<? extends IdsAssetElement> elements) {
        // Remove each AAS element from EDC assetStore as well as its contract
        elements.forEach(element -> {
            resourceController.deleteAsset(element.getIdsAssetId());
            resourceController.deleteContractReference(element.getIdsContractId(), element.getIdsAssetId());
        });
    }

    private List<? extends IdsAssetElement> getAllElements(CustomAssetAdministrationShellEnvironment env) {
        // Collect all assets and contracts associated with this AAS
        var allElements = new ArrayList<IdsAssetElement>();
        allElements.addAll(env.getConceptDescriptions());
        allElements.addAll(env.getAssetAdministrationShells());
        allElements.addAll(env.getSubmodels());
        env.getSubmodels().forEach(submodel -> allElements.addAll(SubmodelUtil.getAllSubmodelElements(submodel)));
        return allElements;
    }
}
