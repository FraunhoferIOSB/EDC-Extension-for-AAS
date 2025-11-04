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
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionChangeListener;
import de.fraunhofer.iosb.repository.AasRepositoryManager;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryConfig;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Handles requests regarding the Asset Administration Shells registered to this
 * extension
 */
public class AasController implements SelfDescriptionChangeListener {

    private final AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager;
    private final Monitor monitor;

    /**
     * Class constructor
     *
     * @param monitor              Logging
     * @param aasRepositoryManager AAS Repository manager implementation to start AAS repositories
     */
    public AasController(Monitor monitor, AasRepositoryManager<FaaastRepositoryConfig> aasRepositoryManager) {
        this.monitor = monitor;
        this.aasRepositoryManager = aasRepositoryManager;
    }

    /**
     * Starts an AAS service internally
     *
     * @param model      AAS Environment for the AAS service
     * @param port       AAS service's exposed HTTP port for communication
     *                   with this extension
     * @param configPath AAS configPath (optional)
     * @return The URL of the new service or null on error
     * @throws URISyntaxException If the URI creation fails
     */
    public URI startService(Path model, int port, Path configPath) throws URISyntaxException {
        FaaastRepositoryConfig config = FaaastRepositoryConfig.Builder.newInstance()
                .model(model)
                .port(port)
                .configPath(configPath)
                .build();

        return aasRepositoryManager.startRepository(config).getUri();
    }

    /**
     * Starts an AAS service internally
     *
     * @param model AAS Environment for the AAS service
     * @return The URL of the new service or null on error
     * @throws URISyntaxException If the URI creation fails
     */
    public URI startService(Path model) throws URISyntaxException {
        FaaastRepositoryConfig config = FaaastRepositoryConfig.Builder.newInstance()
                .model(model)
                .build();

        return aasRepositoryManager.startRepository(config).getUri();
    }

    /**
     * Stops an AAS service given its URL if internally started
     *
     * @param aasServiceUrl URL of service to be stopped
     */
    public void stopService(URI aasServiceUrl) {
        monitor.info("Stopping AAS service with URL %s".formatted(aasServiceUrl.toString()));
        aasRepositoryManager.stopRepository(aasServiceUrl);
    }

    /**
     * Stops all internally started AAS services
     */
    public void stopServices() {
        monitor.info("Stopping all internally started AAS services...");
        aasRepositoryManager.stopAll();
    }

    @Override
    public void removed(Service service) {
        stopService(service.baseUri());
    }

}
