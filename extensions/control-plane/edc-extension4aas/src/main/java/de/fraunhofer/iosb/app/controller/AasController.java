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

import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.aas.AssetAdministrationShellServiceManager;
import de.fraunhofer.iosb.app.aas.FaaastServiceManager;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Handles requests regarding the Asset Administration Shells registered to this
 * extension
 */
public class AasController implements SelfDescriptionChangeListener {

    private final AssetAdministrationShellServiceManager aasServiceManager;
    private final Monitor monitor;

    /**
     * Class constructor
     *
     * @param monitor Logging
     */
    public AasController(Monitor monitor) {
        this.monitor = monitor;
        aasServiceManager = new FaaastServiceManager(monitor);
    }

    /**
     * Starts an AAS service internally
     *
     * @param model  AAS Environment for the AAS service
     * @param port   AAS service's exposed HTTP port for communication
     *               with this extension
     * @param config AAS config (optional)
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    public URL startService(Path model, int port, Path config) throws IOException {
        Objects.requireNonNull(model);

        URL serviceUrl;
        if (config == null && port != 0) {
            monitor.info("Starting AAS service with AAS model (%s) and service port (%s)."
                    .formatted(model, port));
            serviceUrl = aasServiceManager.startService(model, port);
        } else if (config != null) {
            monitor.info("Starting AAS service with AAS model (%s) and service config (%s)."
                    .formatted(model, config));
            serviceUrl = aasServiceManager.startService(model, config);
        } else {
            throw new IllegalArgumentException("Config or port must be specified.");
        }

        return serviceUrl;
    }

    /**
     * Starts an AAS service internally
     *
     * @param model AAS Environment for the AAS service
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    public URL startService(Path model) throws IOException {
        Objects.requireNonNull(model);
        return aasServiceManager.startService(model);
    }

    /**
     * Stops an AAS service given its URL if internally started
     *
     * @param aasServiceUrl URL of service to be stopped
     */
    public void stopService(URL aasServiceUrl) {
        monitor.info("Stopping AAS service with URL %s".formatted(aasServiceUrl.toString()));
        aasServiceManager.stopService(aasServiceUrl);
    }

    /**
     * Stops all internally started AAS services
     */
    public void stopServices() {
        monitor.info("Stopping all internally started AAS services...");
        aasServiceManager.stopServices();
    }

    @Override
    public void removed(Service service) {
        stopService(service.baseUrl());
    }

}
