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

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.internal.MessageBusInternalConfig;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.AASEnvironmentHelper;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import org.eclipse.edc.spi.EdcException;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Manages internally created FA³ST instances.
 */
public class FaaastServiceManager implements AssetAdministrationShellServiceManager {

    private static final String FAAAST_SERVICE_EXCEPTION_MESSAGE = "Exception thrown by FA³ST service.";
    private static final String LOCALHOST_URL = "http://localhost:";

    private final Logger logger;
    private final Map<URL, Service> faaastServiceRepository;

    public FaaastServiceManager() {
        logger = Logger.getInstance();
        faaastServiceRepository = new ConcurrentHashMap<>();
    }

    @Override
    public URL startService(Path aasModelPath, int port) throws IOException {
        Objects.requireNonNull(aasModelPath);
        if (!isValidPort(port)) {
            var errorMessage = format("Port is not valid: (%s).", port);
            logger.error(errorMessage);
            throw new EdcException(errorMessage);
        }
        logger.debug(format("Booting up FA³ST service using AAS model path (%s) and service port (%s).", aasModelPath,
                port));

        try {
            var aasEnvironment = AASEnvironmentHelper.fromFile(aasModelPath.toFile());

            var httpEndpointConfig = new HttpEndpointConfig();
            httpEndpointConfig.setPort(port);

            var serviceConfig = new ServiceConfig.Builder()
                    .core(new CoreConfig.Builder().requestHandlerThreadPoolSize(2).build())
                    .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                    .persistence(new PersistenceInMemoryConfig())
                    .messageBus(new MessageBusInternalConfig())
                    .build();

            var service = new Service(aasEnvironment, serviceConfig);
            service.start();
            logger.debug("Booted up FA³ST service.");

            faaastServiceRepository.put(new URL(LOCALHOST_URL + port), service);

        } catch (Exception faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }

        return new URL(LOCALHOST_URL + port);
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath) throws IOException {
        Objects.requireNonNull(aasModelPath);
        Objects.requireNonNull(configPath);
        return startService(aasModelPath, configPath, -1);
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath, int port) throws IOException {
        if (isValidPort(port)) {
            logger.debug(format("Booting up FA³ST service using AAS model path (%s), FA³ST config path (%s) and service port (%s).",
                    aasModelPath, configPath, port));
        } else {
            logger.debug(format("Booting up FA³ST service using AAS model path (%s) and FA³ST config path (%s).",
                    aasModelPath, configPath));
        }

        var localFaaastServicePort = 0;
        try {
            var aasEnvironment = AASEnvironmentHelper.fromFile(aasModelPath.toFile());

            var serviceConfig = ServiceConfigHelper.load(configPath.toFile());
            var isEndpointsNull = (serviceConfig.getEndpoints() == null); // Remove auto generated httpEndpoint later...
            ServiceConfigHelper.autoComplete(serviceConfig);

            if (isValidPort(port)) {
                if (isEndpointsNull) {
                    serviceConfig.setEndpoints(List.of(new HttpEndpointConfig.Builder().port(port).build()));
                } else {
                    var endpoints = serviceConfig.getEndpoints();
                    endpoints.add(new HttpEndpointConfig.Builder().port(port).build());
                    serviceConfig.setEndpoints(endpoints);
                }
                localFaaastServicePort = port;

            } else {
                for (var endpointConfig : serviceConfig.getEndpoints()) {
                    if (endpointConfig instanceof HttpEndpointConfig) {
                        localFaaastServicePort = ((HttpEndpointConfig) endpointConfig).getPort();
                    }
                }
            }

            // If localFaaastServicePort is unchanged, no valid HTTP Endpoint was found/created.
            if (localFaaastServicePort == 0) {
                throw new IllegalArgumentException(
                        "No HTTP endpoint has been defined in this configuration. Not booting up this FA³ST service.");
            }

            var service = new Service(aasEnvironment, serviceConfig);
            service.start();
            logger.debug("Booted up FA³ST service.");

            faaastServiceRepository.put(new URL(LOCALHOST_URL + localFaaastServicePort), service);

        } catch (
                Exception faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return new URL(LOCALHOST_URL + localFaaastServicePort);
    }

    @Override
    public void stopServices() {
        logger.debug("Shutting down all internally started FA³ST services...");
        faaastServiceRepository.values().forEach(Service::stop);
    }

    @Override
    public void stopService(URL aasServiceUrl) {
        Objects.requireNonNull(aasServiceUrl);
        logger.debug(format("Shutting down FA³ST service with URL %s...", aasServiceUrl));
        var serviceToStop = faaastServiceRepository.get(aasServiceUrl);
        if (Objects.nonNull(serviceToStop)) {
            serviceToStop.stop();
            faaastServiceRepository.remove(aasServiceUrl);
        } else {
            throw new IllegalArgumentException("This URL is not registered as a FA³ST service.");
        }
    }

    private boolean isValidPort(int port) {
        return port < 65536 && port > 0;
    }

}
