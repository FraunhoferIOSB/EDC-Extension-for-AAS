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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Manages a specific type of AAS services like e.g., FA³ST, BaSyx
 */
public interface AssetAdministrationShellServiceManager {

    /**
     * Boot up an AAS service by environment, an open port will be used.
     *
     * @param aasModelPath AAS Environment for the AAS service
     * @return The URL of the new service
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath) throws IOException;

    /**
     * Boot up an AAS service by environment and port
     *
     * @param aasModelPath AAS Environment for the AAS service
     * @param port         AAS service's exposed HTTP port for communication
     *                     with this extension
     * @return The URL of the new service
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath, int port) throws IOException;

    /**
     * Boot up an AAS service by environment and config
     *
     * @param aasModelPath AAS Environment for the AAS service
     * @param configPath   AAS service config
     * @return The URL of the new service
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath, Path configPath) throws IOException;

    /**
     * Boot up an AAS service by environment and config
     *
     * @param aasModelPath AAS Environment for the AAS service
     * @param configPath   AAS service config
     * @param port         AAS service's exposed HTTP port for communication
     *                     with this extension
     * @return The URL of the new service
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath, Path configPath, int port) throws IOException;

    /**
     * Stop all running AAS services that were started by this manager
     */
    void stopServices();

    /**
     * Stop an AAS service by URL
     *
     * @param aasServiceUrl AAS service to be stopped
     */
    void stopService(URL aasServiceUrl);
}
