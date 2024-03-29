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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSemanticId;
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
import io.adminshell.aas.v3.model.Key;
import io.adminshell.aas.v3.model.Reference;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.impl.DefaultKey;
import io.adminshell.aas.v3.model.impl.DefaultReference;
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
        // Make objectMapper recognize DefaultReference as implementation of Reference
        final var simpleModule = new SimpleModule()
                .addAbstractTypeMapping(Reference.class, DefaultReference.class)
                .addAbstractTypeMapping(Key.class, DefaultKey.class);

        objectMapper = JsonMapper
                .builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
        objectMapper.registerModule(simpleModule);
    }

    /**
     * Overwrite aas model element.
     *
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
            logger.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Create aas model element.
     *
     * @param aasServiceUrl AAS service to be updated with path to the new element
     * @param element       New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response postModel(URL aasServiceUrl, String element) {
        Response response;
        try {
            response = Transformer.okHttpResponseToJakartaResponse(httpRestClient.post(aasServiceUrl, element));
        } catch (IOException io) {
            logger.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Create aas model element.
     *
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
            logger.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(response.getStatus()).entity(aasServiceUrl).build();
    }

    /**
     * Returns AAS model enriched with each elements access URL as string in
     * sourceUrl field.
     *
     * @param aasServiceUrl AAS service to be updated
     * @return AAS model enriched with each elements access URL as string in assetId field.
     */
    public CustomAssetAdministrationShellEnvironment getAasEnvWithUrls(URL aasServiceUrl, boolean onlySubmodels)
            throws IOException, DeserializationException {
        var aasServiceUrlString = aasServiceUrl.toString();

        var model = readModel(aasServiceUrl, onlySubmodels);
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
        model.getSubmodels().forEach(submodel -> AASUtil.getAllSubmodelElements(submodel)
                .forEach(element -> element.setSourceUrl(
                        element.getSourceUrl()
                                .replaceFirst("\\.", "/"))));

        // Add urls to all concept descriptions
        model.getConceptDescriptions().forEach(
                conceptDesc -> conceptDesc.setSourceUrl(format("%s/concept-descriptions/%s", aasServiceUrlString,
                        Encoder.encodeBase64(conceptDesc.getIdentification().getId()))));
        return model;
    }

    /**
     * Returns the AAS model.
     */
    private CustomAssetAdministrationShellEnvironment readModel(URL aasServiceUrl, boolean onlySubmodels)
            throws IOException, DeserializationException {
        var aasEnv = new CustomAssetAdministrationShellEnvironment();

        URL shellsUrl;
        URL conceptDescriptionsUrl;
        URL submodelUrl;
        try {
            submodelUrl = aasServiceUrl.toURI().resolve("/submodels").toURL();
            shellsUrl = aasServiceUrl.toURI().resolve("/shells").toURL();
            conceptDescriptionsUrl = aasServiceUrl.toURI().resolve("/concept-descriptions").toURL();
        } catch (URISyntaxException resolveUriException) {
            throw new EdcException(
                    format("Error while building URLs for reading from the AAS Service at %s", aasServiceUrl),
                    resolveUriException);
        }

        aasEnv.setSubmodels(readSubmodels(submodelUrl, onlySubmodels));
        if (!onlySubmodels) {
            aasEnv.setAssetAdministrationShells(readShells(shellsUrl));
            aasEnv.setConceptDescriptions(readConceptDescriptions(conceptDescriptionsUrl));
        }
        return aasEnv;
    }

    private List<CustomConceptDescription> readConceptDescriptions(URL conceptDescriptionsUrl) throws IOException {

        var conceptResponse = Objects.requireNonNull(httpRestClient.get(conceptDescriptionsUrl).body()).string();

        return Arrays.asList(objectMapper.readValue(conceptResponse, CustomConceptDescription[].class));
    }

    private List<CustomAssetAdministrationShell> readShells(URL shellsUrl) throws IOException {
        var shellHttpResponse = Objects.requireNonNull(httpRestClient.get(shellsUrl).body()).string();

        return Arrays.asList(objectMapper.readValue(shellHttpResponse, CustomAssetAdministrationShell[].class));
    }

    private List<CustomSubmodel> readSubmodels(URL submodelUrl, boolean onlySubmodels)
            throws IOException, DeserializationException {
        var submodelHttpResponse = Objects.requireNonNull(httpRestClient.get(submodelUrl).body()).string();

        // First, parse into "full" admin-shell.io submodels:
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        List<DefaultSubmodel> submodels = jsonDeserializer.readReferables(submodelHttpResponse, DefaultSubmodel.class);

        // Now, create customSubmodels from the "full" submodels
        List<CustomSubmodel> customSubmodels = new ArrayList<>();
        for (Submodel submodel : submodels) {
            var customSubmodel = new CustomSubmodel();
            var customIdentification = new Identifier();
            customIdentification.setIdType(submodel.getIdentification().getIdType().toString());
            customIdentification.setId(submodel.getIdentification().getIdentifier());
            customSubmodel.setIdentification(customIdentification);
            customSubmodel.setIdShort(submodel.getIdShort());
            if (Objects.nonNull(submodel.getSemanticId().getKeys())) {
                customSubmodel.setSemanticId(new CustomSemanticId(submodel.getSemanticId().getKeys()));
            }

            if (!onlySubmodels) {
                // Recursively add submodelElements
                var customElements = AASUtil.getCustomSubmodelElementStructureFromSubmodel(submodel);
                customSubmodel.setSubmodelElements((List<CustomSubmodelElement>) customElements);
            }
            customSubmodels.add(customSubmodel);
        }
        return customSubmodels;
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
                newCollectionElements.add(putUrlRec(format("%s.%s", url, element.getIdShort()), collectionElement));
            }
            ((CustomSubmodelElementCollection) element).setValues(newCollectionElements);
        }
        element.setSourceUrl(format("%s.%s", url, element.getIdShort()));
        return element;
    }
}
