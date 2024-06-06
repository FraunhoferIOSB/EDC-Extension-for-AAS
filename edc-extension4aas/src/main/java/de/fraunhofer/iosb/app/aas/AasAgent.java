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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElement;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementCollection;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodelElementList;
import de.fraunhofer.iosb.app.util.AssetAdministrationShellUtil;
import de.fraunhofer.iosb.app.util.HttpRestClient;
import jakarta.ws.rs.core.Response;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Communicating with AAS service
 */
public class AasAgent {

    private final Monitor monitor;

    private final HttpRestClient httpRestClient = HttpRestClient.getInstance();
    private final JsonDeserializer jsonDeserializer;
    private final Map<String, Certificate[]> acceptedCertificates;

    private final ObjectMapper objectMapper;

    public AasAgent(Monitor monitor) {
        this.monitor = monitor;
        jsonDeserializer = new JsonDeserializer();
        acceptedCertificates = new HashMap<>();
        objectMapper = new ObjectMapper();
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

        try (okhttp3.Response response = httpRestClient.put(aasServiceUrl, element, monitor)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();

        } catch (IOException io) {
            monitor.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create aas model element.
     *
     * @param aasServiceUrl AAS service to be updated with path to the new element
     * @param element       New AAS model element.
     * @return String containing response of AAS service.
     */
    public Response postModel(URL aasServiceUrl, String element) {

        try (okhttp3.Response response = httpRestClient.post(aasServiceUrl, element, monitor)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();

        } catch (IOException io) {
            monitor.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
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

        try (okhttp3.Response response = httpRestClient.delete(aasServiceUrl, element, monitor)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();
        } catch (IOException io) {
            monitor.severe("Could not fetch AAS env from AAS service", io);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

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
        var aasServiceUrlString = format("%s/api/v3.0", aasServiceUrl);

        var environment = readEnvironment(aasServiceUrl, onlySubmodels);

        environment.getAssetAdministrationShells().forEach(shell -> {
            shell.setSourceUrl(aasServiceUrlString);
            shell.setReferenceChain(
                    new DefaultReference.Builder()
                            .keys(new DefaultKey.Builder()
                                    .type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                                    .value(shell.getId())
                                    .build())
                            .build());
        });

        environment.getConceptDescriptions().forEach(conceptDesc -> {
            conceptDesc.setSourceUrl(aasServiceUrlString);
            conceptDesc.setReferenceChain(
                    new DefaultReference.Builder()
                            .keys(new DefaultKey.Builder()
                                    .type(KeyTypes.CONCEPT_DESCRIPTION)
                                    .value(conceptDesc.getId())
                                    .build())
                            .build());
        });

        environment.getSubmodels().forEach(submodel -> {
            submodel.setSourceUrl(aasServiceUrlString);
            submodel.setReferenceChain(
                    new DefaultReference.Builder()
                            .keys(createKey(KeyTypes.SUBMODEL, submodel.getId()))
                            .build());
            submodel.getSubmodelElements()
                    .forEach(elem -> putUrl(aasServiceUrlString,
                            submodel.getReferenceChain(),
                            elem));
        });

        return environment;
    }

    /**
     * Returns the AAS environment.
     */
    private CustomAssetAdministrationShellEnvironment readEnvironment(URL aasServiceUrl, boolean onlySubmodels)
            throws IOException, DeserializationException {
        var aasEnv = new CustomAssetAdministrationShellEnvironment();

        URL submodelUrl;
        URL shellsUrl;
        URL conceptDescriptionsUrl;
        try {
            submodelUrl = aasServiceUrl.toURI().resolve("/api/v3.0/submodels").toURL();
            shellsUrl = aasServiceUrl.toURI().resolve("/api/v3.0/shells").toURL();
            conceptDescriptionsUrl = aasServiceUrl.toURI().resolve("/api/v3.0/concept-descriptions").toURL();
        } catch (URISyntaxException resolveUriException) {
            throw new EdcException(
                    format("Error while building URLs for reading from the AAS service at %s", aasServiceUrl),
                    resolveUriException);
        }

        aasEnv.setSubmodels(readSubmodels(submodelUrl, onlySubmodels));
        if (!onlySubmodels) {
            aasEnv.setAssetAdministrationShells(readShells(shellsUrl));
            aasEnv.setConceptDescriptions(readConceptDescriptions(conceptDescriptionsUrl));
        }
        return aasEnv;
    }

    private List<CustomConceptDescription> readConceptDescriptions(URL conceptDescriptionsUrl) throws IOException, DeserializationException {
        var element = readAssetAdministrationShellElement(conceptDescriptionsUrl, ConceptDescription.class);

        return element.stream()
                .map(CustomConceptDescription::fromConceptDescription)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<CustomAssetAdministrationShell> readShells(URL shellsUrl) throws IOException, DeserializationException {
        var element = readAssetAdministrationShellElement(shellsUrl, AssetAdministrationShell.class);

        return element.stream()
                .map(CustomAssetAdministrationShell::fromAssetAdministrationShell)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<CustomSubmodel> readSubmodels(URL submodelUrl, boolean onlySubmodels) throws IOException, DeserializationException {
        var submodels = readAssetAdministrationShellElement(submodelUrl, Submodel.class);

        // Now, create customSubmodels from the "full" submodels
        List<CustomSubmodel> customSubmodels = new ArrayList<>();
        for (Submodel submodel : submodels) {
            var customSubmodel = new CustomSubmodel();
            customSubmodel.setId(submodel.getId());
            customSubmodel.setIdShort(submodel.getIdShort());
            if (Objects.nonNull(submodel.getSemanticId())) {
                customSubmodel.setSemanticId(submodel.getSemanticId());
            }

            if (!onlySubmodels) {
                // Recursively add submodelElements
                var customElements = AssetAdministrationShellUtil.getCustomSubmodelElementStructureFromSubmodel(submodel);
                customSubmodel.setSubmodelElements((List<CustomSubmodelElement>) customElements);
            }
            customSubmodels.add(customSubmodel);
        }
        return customSubmodels;
    }

    private <T extends Identifiable> List<T> readAssetAdministrationShellElement(URL contentUrl, Class<T> clazz) throws IOException, DeserializationException {
        var response = Objects.requireNonNull(httpRestClient.get(contentUrl, monitor).body()).string();
        var responseJson = objectMapper.readTree(response).get("result");

        return Objects.isNull(responseJson) ? List.of() : jsonDeserializer.readList(responseJson, clazz);
    }

    /**
     * Add the access url + refChain of this element to its sourceUrl field. If this element
     * is a collection/list, do this recursively for all elements inside this collection/list,
     * too (since we don't know how deeply nested the collection is).
     */
    private void putUrl(String url, Reference parentReferenceChain, CustomSubmodelElement element) {
        var ownReferenceChainBuilder = new DefaultReference.Builder()
                .keys(new ArrayList<>(parentReferenceChain.getKeys()));
        Reference ownReferenceChain;

        // "value" field of a submodel element can be an array or not available (no collection)
        if (element instanceof CustomSubmodelElementCollection) {
            ownReferenceChain = ownReferenceChainBuilder
                    .keys(createKey(KeyTypes.SUBMODEL_ELEMENT_COLLECTION,
                            element.getIdShort()))
                    .build();

            ((CustomSubmodelElementCollection) element).getValue().forEach(item -> putUrl(url, ownReferenceChain, item));

        } else if (element instanceof CustomSubmodelElementList) {
            ownReferenceChain = ownReferenceChainBuilder
                    .keys(createKey(KeyTypes.SUBMODEL_ELEMENT_LIST,
                            element.getIdShort()))
                    .build();
            ((CustomSubmodelElementList) element).getValue().forEach(item -> putUrl(url, ownReferenceChain, item));

        } else { // Can not have any child elements...
            ownReferenceChain = ownReferenceChainBuilder
                    .keys(createKey(KeyTypes.SUBMODEL_ELEMENT,
                            element.getIdShort())).build();
        }

        element.setSourceUrl(url);
        element.setReferenceChain(ownReferenceChain);
    }

    private Key createKey(KeyTypes type, String value) {
        return new DefaultKey.Builder().type(type).value(value).build();
    }


    public void addCertificates(URL url, Certificate[] certs) throws KeyStoreException, NoSuchAlgorithmException {
        acceptedCertificates.put(url.toString(), certs);
        httpRestClient.setAcceptedSelfSignedCertificates(acceptedCertificates);
    }

    public void removeCertificates(URL aasServiceUrl) {
        acceptedCertificates.remove(aasServiceUrl.toString());
        // Here we don't throw the exception on since we should still remove
        // the service even if we cannot remove its self-signed certificates.
        try {
            httpRestClient.setAcceptedSelfSignedCertificates(acceptedCertificates);
        } catch (KeyStoreException | NoSuchAlgorithmException generalSecurityException) {
            throw new EdcException(
                    format("Exception thrown while trying to remove certificate by %s", aasServiceUrl),
                    generalSecurityException);
        }
    }
}
