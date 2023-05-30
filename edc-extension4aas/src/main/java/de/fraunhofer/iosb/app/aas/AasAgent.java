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
import de.fraunhofer.iosb.app.util.AASUtil;
import de.fraunhofer.iosb.app.util.Encoder;
import de.fraunhofer.iosb.app.util.HttpRestClient;
import de.fraunhofer.iosb.app.util.Transformer;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.dataformat.json.JsonDeserializer;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.impl.DefaultSubmodel;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Communicating with AAS service
 */
public class AasAgent {

    private final HttpRestClient httpRestClient;
    private final Logger logger;
    // Object Mapper to write model as self description using custom aas model
    private final ObjectMapper objectMapper;

    public AasAgent(OkHttpClient client) {
        Objects.requireNonNull(client);
        this.httpRestClient = new HttpRestClient(client);
        logger = Logger.getInstance();
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Overwrite aas model element.

     * @param aasServiceUrl AAS service to be updated with the path to the updated
     *                      element
     * @param element       Updated AAS model element.
     * @return String containing response of AAS service.
     */
    public Response putModel(URL aasServiceUrl, String element) {
        Response response;
        try {
            response = Transformer.okHttpResponseToJakartaResponse(httpRestClient.put(aasServiceUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Create aas model element.

     * @param aasServiceUrl AAS service to be updated with path to the new element
     * @param element       New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response postModel(URL aasServiceUrl, String element) {
        Response response;
        try {
            response = Transformer.okHttpResponseToJakartaResponse(httpRestClient.post(aasServiceUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Create aas model element.

     * @param aasServiceUrl AAS service to be updated with the path to the element
     *                      to be removed
     * @param element       New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response deleteModel(URL aasServiceUrl, String element) {
        Response response;
        try {
            response = Transformer.okHttpResponseToJakartaResponse(httpRestClient.delete(aasServiceUrl, element));
        } catch (IOException io) {
            logger.error("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Returns AAS model enriched with each elements access URL as string in
     * sourceUrl field.

     * @param aasServiceUrl AAS service to be updated
     * @return AAS model enriched with each elements access URL as string in assetId
     *         field.
     */
    public CustomAssetAdministrationShellEnvironment getAasEnvWithUrls(URL aasServiceUrl)
            throws IOException, DeserializationException {
        var aasServiceUrlString = aasServiceUrl.toString();

        var model = readModel(aasServiceUrl);
        // Add urls to all shells
        model.getAssetAdministrationShells().forEach(shell -> shell.setSourceUrl(
                format("%s/shells/%s", aasServiceUrlString,
                        Encoder.encodeBase64(shell.getIdentification().getId()))));
        // Add urls to all submodels, submodelElements
        model.getSubmodels().forEach(submodel -> {
            submodel.setSourceUrl(
                    format("%s/submodels/%s", aasServiceUrlString,
                            Encoder.encodeBase64(submodel.getIdentification().getId())));
            submodel.getSubmodelElements()
                    .forEach(elem -> putUrlRec(
                            format("%s/submodels/%s/submodel/submodel-elements", aasServiceUrlString,
                                    Encoder.encodeBase64(submodel.getIdentification().getId())),
                            elem));
        });
        // Add urls to all concept descriptions
        model.getConceptDescriptions().forEach(
                conceptDesc -> conceptDesc.setSourceUrl(format("%s/concept-descriptions/%s", aasServiceUrlString,
                        Encoder.encodeBase64(conceptDesc.getIdentification().getId()))));
        return model;
    }

    /**
     * Returns the AAS model.
     */
    private CustomAssetAdministrationShellEnvironment readModel(URL aasServiceUrl)
            throws IOException, DeserializationException {

        String shellResponse;
        String conceptResponse;
        String submodelResponse;
        try {
            shellResponse = Objects.requireNonNull(httpRestClient.get(aasServiceUrl.toURI().resolve("/shells").toURL()).body()).string();
            submodelResponse = Objects.requireNonNull(httpRestClient.get(aasServiceUrl.toURI().resolve("/submodels").toURL()).body()).string();
            conceptResponse = Objects.requireNonNull(httpRestClient.get(aasServiceUrl.toURI().resolve("/concept-descriptions").toURL()).body())
                    .string();
        } catch (URISyntaxException e) {
            throw new EdcException(e.getMessage());
        }

        CustomAssetAdministrationShell[] shells = objectMapper.readValue(shellResponse,
                CustomAssetAdministrationShell[].class);
        CustomConceptDescription[] conceptDescriptions = objectMapper.readValue(conceptResponse,
                CustomConceptDescription[].class);

        // Because of SMCs "value" field, submodels have to be parsed manually

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
            var customElements = AASUtil.getCustomSubmodelElementStructureFromSubmodel(submodel);
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
                newCollectionElements.add(putUrlRec(format("%s/%s", url, element.getIdShort()), collectionElement));
            }
            ((CustomSubmodelElementCollection) element).setValues(newCollectionElements);
        }
        element.setSourceUrl(format("%s/%s", url, element.getIdShort()));
        return element;
    }
}
