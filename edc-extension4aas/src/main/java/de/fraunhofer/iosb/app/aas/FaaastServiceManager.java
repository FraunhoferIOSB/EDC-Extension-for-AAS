package de.fraunhofer.iosb.app.aas;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.aas.repository.AssetAdministrationShellServiceRepository;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.internal.MessageBusInternalConfig;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.AASEnvironmentHelper;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Manages internally created FA³ST instances.
 */
public class FaaastServiceManager implements AssetAdministrationShellServiceManager {

    private static final String FAAAST_SERVICE_EXCEPTION_MESSAGE = "Exception thrown by FA³ST client.";
    private static final String LOCALHOST_URL = "http://localhost:";

    private final Logger logger;
    private AssetAdministrationShellServiceRepository<Service> faaastServiceRepository;

    public FaaastServiceManager() {
        logger = Logger.getInstance();
        faaastServiceRepository = new AssetAdministrationShellServiceRepository<>();
    }

    @Override
    public URL startService(Path aasModelPath, int port) throws IOException {
        Objects.requireNonNull(aasModelPath);

        try {
            var aasEnvironment = AASEnvironmentHelper.fromFile(aasModelPath.toFile());
            ServiceConfig serviceConfig;

            final var httpEndpointConfig = new HttpEndpointConfig();
            httpEndpointConfig.setPort(port);

            serviceConfig = new ServiceConfig.Builder()
                    .core(new CoreConfig.Builder().requestHandlerThreadPoolSize(2).build())
                    .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                    .persistence(new PersistenceInMemoryConfig())
                    .messageBus(new MessageBusInternalConfig())
                    .build();

            var service = new Service(aasEnvironment, serviceConfig);
            service.start();
            logger.debug("Booted up FA³ST service.");

            faaastServiceRepository.add(new URL(LOCALHOST_URL + String.valueOf(port)), service);

        } catch (Exception faaastServiceException) {
            logger.error(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }

        return new URL(LOCALHOST_URL + String.valueOf(port));
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath) throws IOException {
        Objects.requireNonNull(aasModelPath);
        Objects.requireNonNull(configPath);

        var localFaaastServicePort = 0;
        try {
            var aasEnvironment = AASEnvironmentHelper.fromFile(aasModelPath.toFile());
            ServiceConfig serviceConfig;

            serviceConfig = ServiceConfigHelper.load(configPath.toFile());
            ServiceConfigHelper.autoComplete(serviceConfig);

            for (final var endpointConfig : serviceConfig.getEndpoints()) {
                if (endpointConfig instanceof HttpEndpointConfig) {
                    localFaaastServicePort = ((HttpEndpointConfig) endpointConfig).getPort();
                }
            }

            var service = new Service(aasEnvironment, serviceConfig);
            service.start();
            logger.debug("Booted up FA³ST service.");

            faaastServiceRepository.add(new URL(LOCALHOST_URL + String.valueOf(localFaaastServicePort)), service);

        } catch (Exception faaastServiceException) {
            logger.error(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return new URL(LOCALHOST_URL + String.valueOf(localFaaastServicePort));
    }
    
    @Override
    public void stopServices() {
        logger.log("Shutting down all internally started FAAAST Clients...");
        faaastServiceRepository.getAllValues().forEach(service -> service.stop());
    }

    @Override
    public void stopService(URL aasServiceUrl) {
        var serviceToStop = faaastServiceRepository.getService(aasServiceUrl);
        if (Objects.nonNull(serviceToStop)) {
            serviceToStop.stop();
            faaastServiceRepository.remove(aasServiceUrl);
        }
    }

}
