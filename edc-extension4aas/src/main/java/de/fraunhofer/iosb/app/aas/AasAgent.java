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

import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShell;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import de.fraunhofer.iosb.app.model.aas.CustomConceptDescription;
import de.fraunhofer.iosb.app.model.aas.CustomSubmodel;
import de.fraunhofer.iosb.app.util.HttpRestClient;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Communicating with AAS service
 */
public class AasAgent {

    private final Monitor monitor;

    private final HttpRestClient httpRestClient = HttpRestClient.getInstance();
    private final Map<String, Certificate[]> acceptedCertificates;


    private ModelParser modelParser;

    public AasAgent(Monitor monitor) {
        this.monitor = monitor;
        acceptedCertificates = new HashMap<>();
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
            throws IOException {
        var aasServiceUrlString = format("%s/api/v3.0", aasServiceUrl);
        modelParser = new ModelParser(aasServiceUrlString);

        return readEnvironment(aasServiceUrl, onlySubmodels);
    }

    public void addCertificates(URL url, Certificate[] certs) throws KeyStoreException {
        acceptedCertificates.put(url.toString(), certs);
        httpRestClient.setAcceptedSelfSignedCertificates(acceptedCertificates);
    }

    public void removeCertificates(URL aasServiceUrl) {
        acceptedCertificates.remove(aasServiceUrl.toString());
        // Here we don't throw the exception on since we should still remove
        // the service even if we cannot remove its self-signed certificates.
        try {
            httpRestClient.setAcceptedSelfSignedCertificates(acceptedCertificates);
        } catch (KeyStoreException generalSecurityException) {
            throw new EdcException(
                    format("Exception thrown while trying to remove certificate by %s", aasServiceUrl),
                    generalSecurityException);
        }
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
        try (var response = httpRestClient.get(conceptDescriptionsUrl, monitor)) {
            var body = response.body();
            if (body == null) {
                throw new EdcException("Received empty body for concept description request");
            }
            return modelParser.parseConceptDescriptions(body.string());
        }
    }

    private List<CustomAssetAdministrationShell> readShells(URL shellsUrl) throws IOException {
        try (var response = httpRestClient.get(shellsUrl, monitor)) {
            var body = response.body();
            if (body == null) {
                throw new EdcException("Received empty body for shell request");
            }
            return modelParser.parseShells(body.string());
        }
    }

    private List<CustomSubmodel> readSubmodels(URL submodelUrl, boolean onlySubmodels) throws IOException {
        try (var response = httpRestClient.get(submodelUrl, monitor)) {
            var body = response.body();
            if (body == null) {
                throw new EdcException("Received empty body for submodel request");
            }
            return modelParser.parseSubmodels(body.string(), onlySubmodels);
        }
    }

}
