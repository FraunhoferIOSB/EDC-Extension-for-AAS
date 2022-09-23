package de.fraunhofer.iosb.app.aas;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

/**
 * Manages a specific type of AAS service like e.g., FA³ST, BaSyx
 */
public interface AssetAdministrationShellServiceManager {

    /**
     * Boot up an AAS service by environment and port

     * @param aasModelPath AAS Environment for the AAS service
     * @param port         AAS service's exposed HTTP port for communication
     *                     with this extension
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath, int port) throws IOException;

    /**
     * Boot up an AAS service by environment and config

     * @param aasModelPath AAS Environment for the AAS service
     * @param configPath   AAS service config
     * @return The URL of the new service or null on error
     * @throws IOException If the URL creation fails
     */
    URL startService(Path aasModelPath, Path configPath) throws IOException;

    /**
     * Stop all running AAS services that were started by this manager
     */
    void stopServices();

    /**
     * Stop an AAS service by URL

     * @param aasServiceUrl AAS service to be stopped
     */
    void stopService(URL aasServiceUrl);
}
