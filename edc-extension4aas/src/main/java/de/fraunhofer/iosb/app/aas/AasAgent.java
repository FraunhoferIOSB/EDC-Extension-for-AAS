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

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static jakarta.ws.rs.HttpMethod.DELETE;
import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.POST;
import static jakarta.ws.rs.HttpMethod.PUT;
import static java.lang.String.format;

/**
 * Communicating with AAS service
 */
public class AasAgent {

    private static final int INTERNAL_SERVER_ERROR = 500;

    private final Monitor monitor;
    private final AasDataProcessorFactory aasDataProcessorFactory;

    private ModelParser modelParser;

    public AasAgent(Monitor monitor, AasDataProcessorFactory aasDataProcessorFactory) {
        this.monitor = monitor;
        this.aasDataProcessorFactory = aasDataProcessorFactory;
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
        try (var response = executeRequest(aasServiceUrl, element, PUT)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();
        } catch (IOException io) {
            monitor.severe(format("Could not execute %s request to %s", POST, aasServiceUrl), io);
            return Response.serverError().build();
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
        try (var response = executeRequest(aasServiceUrl, element, POST)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();
        } catch (IOException io) {
            monitor.severe(format("Could not execute %s request to %s", POST, aasServiceUrl), io);
            return Response.serverError().build();
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
        try (var response = executeRequest(aasServiceUrl, element, DELETE)) {
            return Response.status(response.code()).entity(aasServiceUrl).build();
        } catch (IOException io) {
            monitor.severe(format("Could not execute %s request to %s", POST, aasServiceUrl), io);
            return Response.serverError().build();
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
            throws IOException {
        var aasServiceUrlString = format("%s/api/v3.0", aasServiceUrl);
        modelParser = new ModelParser(aasServiceUrlString);

        return readEnvironment(aasServiceUrl, onlySubmodels);
    }

    /**
     * Returns the AAS environment.
     */
    private CustomAssetAdministrationShellEnvironment readEnvironment(URL aasServiceUrl, boolean onlySubmodels)
            throws IOException {
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

    private List<CustomConceptDescription> readConceptDescriptions(URL conceptDescriptionsUrl) throws IOException {
        try (var response = executeRequest(conceptDescriptionsUrl)) {
            var body = response.body();
            if (body == null || response.code() == INTERNAL_SERVER_ERROR) {
                throw new EdcException("Received empty body for concept description request");
            }
            return modelParser.parseConceptDescriptions(body.string());
        }
    }

    private List<CustomAssetAdministrationShell> readShells(URL shellsUrl) throws IOException {
        try (var response = executeRequest(shellsUrl)) {
            var body = response.body();
            if (body == null || response.code() == INTERNAL_SERVER_ERROR) {
                throw new EdcException("Received empty body for shell request");
            }
            return modelParser.parseShells(body.string());
        }
    }

    private List<CustomSubmodel> readSubmodels(URL submodelUrl, boolean onlySubmodels) throws IOException {
        try (var response = executeRequest(submodelUrl)) {
            var body = response.body();
            if (body == null || response.code() == INTERNAL_SERVER_ERROR) {
                throw new EdcException("Received empty body for submodel request");
            }
            return modelParser.parseSubmodels(body.string(), onlySubmodels);
        }
    }

    private okhttp3.Response executeRequest(URL aasServiceUrl) throws IOException {
        return executeRequest(aasServiceUrl, null, GET);
    }

    private okhttp3.Response executeRequest(URL aasServiceUrl, String element, String httpMethod) throws IOException {
        return aasDataProcessorFactory.processorFor(aasServiceUrl.toString())
                .send(AasDataAddress.Builder
                                .newInstance()
                                .method(httpMethod)
                                .baseUrl(aasServiceUrl.toString())
                                .build(),
                        element,
                        MediaType.APPLICATION_JSON);
    }
}
