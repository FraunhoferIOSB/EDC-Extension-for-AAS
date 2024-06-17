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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.RequestType;
import de.fraunhofer.iosb.app.aas.AasAgent;
import de.fraunhofer.iosb.app.aas.AssetAdministrationShellServiceManager;
import de.fraunhofer.iosb.app.aas.FaaastServiceManager;
import de.fraunhofer.iosb.app.aas.ssl.SelfSignedCertificateRetriever;
import de.fraunhofer.iosb.app.model.aas.CustomAssetAdministrationShellEnvironment;
import jakarta.ws.rs.core.Response;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Handles requests regarding the Asset Administration Shells registered to this
 * extension
 */
public class AasController implements Controllable {

    private static final String HTTPS = "https";
    private final AasAgent aasAgent;
    private final AssetAdministrationShellServiceManager aasServiceManager;
    private final Monitor monitor;

    public AasController(Monitor monitor) {
        this.monitor = monitor;

        aasAgent = new AasAgent(monitor);
        aasServiceManager = new FaaastServiceManager(monitor);
    }

    @Override
    public Response handleRequest(RequestType requestType, URL url, String... requestData) {
        return switch (requestType) {
            case POST -> aasAgent.postModel(url, requestData[0]);
            case PUT -> aasAgent.putModel(url, requestData[0]);
            case DELETE -> aasAgent.deleteModel(url, requestData[0]);
            default -> Response.status(Response.Status.NOT_IMPLEMENTED).build();
        };
    }

    /**
     * Returns the AAS model of the AAS service behind the aasServiceUrl, as a
     * self-description (see model/aas/*). This model has the access URL of
     * each AAS element in the sourceUrl field.
     *
     * @param aasServiceUrl url of the service
     * @param onlySubmodels Don't get shells, concept descriptions, submodel elements
     * @return aasServiceUrl's model, in self-description form
     * @throws DeserializationException AAS from service could not be deserialized
     * @throws IOException              Communication with AAS service failed
     */
    public CustomAssetAdministrationShellEnvironment getAasModelWithUrls(URL aasServiceUrl, boolean onlySubmodels)
            throws IOException, DeserializationException {
        Objects.requireNonNull(aasServiceUrl);

        return aasAgent.getAasEnvWithUrls(aasServiceUrl, onlySubmodels);
    }

    /**
     * Starts an AAS service internally
     *
     * @param aasModelPath   AAS Environment for the AAS service
     * @param aasServicePort AAS service's exposed HTTP port for communication
     *                       with this extension
     * @param aasConfigPath  AAS config (optional)
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    public URL startService(Path aasModelPath, int aasServicePort, Path aasConfigPath) throws IOException {
        Objects.requireNonNull(aasModelPath);

        URL serviceUrl;
        if (Objects.isNull(aasConfigPath)) {
            monitor.info(format(
                    "Booting up AAS service given AAS model path (%s)\n and service port (%s)\n...",
                    aasModelPath, aasServicePort));
            serviceUrl = aasServiceManager.startService(aasModelPath, aasServicePort);
        } else {
            monitor.info(format(
                    "Booting up AAS service given AAS model path (%s)\n and service config path (%s)...",
                    aasModelPath, aasConfigPath));
            serviceUrl = aasServiceManager.startService(aasModelPath, aasConfigPath, aasServicePort);
        }

        return serviceUrl;
    }

    /**
     * Stops an AAS service given its URL if internally started
     *
     * @param aasServiceUrl URL of service to be stopped
     */
    public void stopAssetAdministrationShellService(URL aasServiceUrl) {
        monitor.info(format("Shutting down AAS service with URL %s...", aasServiceUrl.toString()));

        removeCertificates(aasServiceUrl);

        aasServiceManager.stopService(aasServiceUrl);
    }

    /**
     * Stops all internally started AAS services
     */
    public void stopServices() {
        monitor.info("Shutting down all AAS services...");
        aasServiceManager.stopServices();
    }

    public Result<Void> addCertificates(URL aasServiceUrl) {
        // TODO rename to something like "make extension accept this url" since http services and services with "real" certificates should also yield a successful result
        // Check if HTTPS and self-signed certificate both apply
        if (!aasServiceUrl.getProtocol().equalsIgnoreCase(HTTPS)) {
            return Result.success();
        }

        try {
            var certs = SelfSignedCertificateRetriever.getSelfSignedCertificate(aasServiceUrl);
            aasAgent.addCertificates(aasServiceUrl, certs);
        } catch (KeyStoreException | NoSuchAlgorithmException | IOException addCertificateException) {
            // This means we probably cannot communicate with the server... warn user
            monitor.warning("Could not add service's certificate to trust manager, communication will probably not be possible.", addCertificateException);
            return Result.failure(addCertificateException.getMessage());
        }

        return Result.success();
    }

    public void removeCertificates(URL aasServiceUrl) {
        aasAgent.removeCertificates(aasServiceUrl);
    }
}
