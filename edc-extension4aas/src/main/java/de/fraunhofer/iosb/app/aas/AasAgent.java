package de.fraunhofer.iosb.app.aas;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import de.fraunhofer.iosb.app.model.aas.Identifier;
import de.fraunhofer.iosb.app.model.aas.util.SubmodelUtil;
import de.fraunhofer.iosb.app.util.Encoder;
import de.fraunhofer.iosb.app.util.HttpRestClient;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.dataformat.json.JsonDeserializer;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.impl.DefaultSubmodel;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.EdcException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Communicating with AAS service
 */
public class AasAgent {

    private final HttpRestClient httpRestClient;
    private final Logger logger;
    // Object Mapper to write model as self description using custom aas model
    private final ObjectMapper objectMapper;

    public AasAgent(OkHttpClient client) {
        this.httpRestClient = new HttpRestClient(client);
        logger = Logger.getInstance();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Overwrite aas model element.

     * @param aasServiceUrl AAS service to be updated
     * @param path    Path to the new element
     * @param element Updated AAS model element.
     * @return String containing response of AAS service.
     */
    public Response putModel(URL aasServiceUrl, String path, String element) {
        URL requestUrl;
        try {
            requestUrl = aasServiceUrl.toURI().resolve(path).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new EdcException(e.getMessage());
        }

        Response response;
        try {
            response = httpRestClient.toJakartaResponse(httpRestClient.put(requestUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(requestUrl).build();
    }

    /**
     * Create aas model element.

     * @param aasServiceUrl AAS service to be updated
     * @param path    Path to the new element
     * @param element New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response postModel(URL aasServiceUrl, String path, String element) {
        URL requestUrl;
        try {
            requestUrl = aasServiceUrl.toURI().resolve(path).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new EdcException(e.getMessage());
        }

        Response response;
        try {
            response = httpRestClient.toJakartaResponse(httpRestClient.put(requestUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(requestUrl).build();
    }

    /**
     * Create aas model element.

     * @param aasServiceUrl AAS service to be updated
     * @param path    Path to the new element
     * @param element New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response deleteModel(URL aasServiceUrl, String path, String element) {
        URL requestUrl;
        try {
            requestUrl = aasServiceUrl.toURI().resolve(path).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new EdcException(e.getMessage());
        }

        Response response;
        try {
            response = httpRestClient.toJakartaResponse(httpRestClient.put(requestUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(requestUrl).build();
    }

    /**
     * Returns the AAS model.

     * @param aasServiceUrl URl to AAS service
     * @return The service's AAS model.
     */
    private CustomAssetAdministrationShellEnvironment readModel(URL aasServiceUrl)
            throws IOException, DeserializationException {

        String shellResponse;
        String conceptResponse;
        String submodelResponse;
        try {
            shellResponse = httpRestClient.get(aasServiceUrl.toURI().resolve("/shells").toURL()).body().string();
            submodelResponse = httpRestClient.get(aasServiceUrl.toURI().resolve("/submodels").toURL()).body().string();
            conceptResponse = httpRestClient.get(aasServiceUrl.toURI().resolve("/concept-descriptions").toURL()).body()
                    .string();
        } catch (URISyntaxException e) {
            throw new EdcException(e.getMessage());
        }

        CustomAssetAdministrationShell[] shells = objectMapper.readValue(shellResponse,
                CustomAssetAdministrationShell[].class);
        CustomConceptDescription[] conceptDescriptions = objectMapper.readValue(conceptResponse,
                CustomConceptDescription[].class);

        // Because of SMC's "value" field, submodels have to be parsed manually

        // First, parse into full admin-shell.io submodels:
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        List<DefaultSubmodel> submodels = jsonDeserializer.readReferables(submodelResponse, DefaultSubmodel.class);

        // Now, create custom submodels from the data of the full submodels
        List<CustomSubmodel> customSubmodels = new ArrayList<>();
        for (Submodel submodel : submodels) {
            var customSubmodel = new CustomSubmodel();
            var customIdentification = new Identifier();
            customIdentification.setIdType(submodel.getIdentification().getIdType().toString());
            customIdentification.setId(submodel.getIdentification().getIdentifier());
            customSubmodel.setIdentification(customIdentification);

            customSubmodel.setIdShort(submodel.getIdShort());

            // Recursively add submodelElements
            var customElements = SubmodelUtil.getCustomSubmodelElementStructureWithUrlsFromSubmodel(submodel);
            customSubmodel.setSubmodelElements((List<CustomSubmodelElement>) customElements);

            customSubmodels.add(customSubmodel);
        }
        var aasEnv = new CustomAssetAdministrationShellEnvironment();

        aasEnv.setAssetAdministrationShells(Arrays.asList(shells));
        aasEnv.setSubmodels(customSubmodels);
        aasEnv.setConceptDescriptions(Arrays.asList(conceptDescriptions));

        return aasEnv;
    }

    /**
     * Returns AAS model enriched with each elements access URL as string in assetId
     * field.

     * @param aasServiceUrl AAS service to be updated
     * @return AAS model enriched with each elements access URL as string in assetId
     *         field.
     */
    public CustomAssetAdministrationShellEnvironment getAasEnvWithUrls(URL aasServiceUrl)
            throws IOException, DeserializationException {
        var aasServiceUrlString = aasServiceUrl.toString();
        // Map <reference, URL>
        var model = readModel(aasServiceUrl);
        // Add urls to all shells
        model.getAssetAdministrationShells().forEach(shell -> {
            shell.setIdsAssetId(
                    aasServiceUrlString + "/shells/" + Encoder.encodeBase64(shell.getIdentification().getId()));
        });
        // Add urls to all submodels, submodelElements
        model.getSubmodels().forEach(submodel -> {
            submodel.setIdsAssetId(
                    aasServiceUrlString + "/submodels/" + Encoder.encodeBase64(submodel.getIdentification().getId()));
            submodel.getSubmodelElements()
                    .forEach(elem -> elem = putUrlRec(aasServiceUrlString + "/submodels/" +   
                            Encoder.encodeBase64(submodel.getIdentification().getId()) +
                            "/submodel/submodel-elements", elem));
        });
        // Add urls to all concept descriptions
        model.getConceptDescriptions().forEach(conceptDesc -> conceptDesc.setIdsAssetId(aasServiceUrlString +
                "/concept-descriptions/" + Encoder.encodeBase64(conceptDesc.getIdentification().getId())));
        return model;
    }

    /**
     * Add the access url of this element to its contract ID field. If this element
     * is a collection, do this recursively for all elements inside this collection,
     * too.
     */
    private CustomSubmodelElement putUrlRec(String url, CustomSubmodelElement element) {
        // "value" field of a submodel element can be string or array, this is why
        // a type check has been added here
        if (element instanceof CustomSubmodelElementCollection) {
            Collection<CustomSubmodelElement> newCollectionElements = new ArrayList<>();
            for (var collectionElement : ((CustomSubmodelElementCollection) element).getValue()) {
                newCollectionElements.add(putUrlRec(url + "/" + element.getIdShort(), collectionElement));
            }
            ((CustomSubmodelElementCollection) element).setValues(newCollectionElements);
        }
        element.setIdsAssetId(url + "/" + element.getIdShort());
        return element;
    }
}
