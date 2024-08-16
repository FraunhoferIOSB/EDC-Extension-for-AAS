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

import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;

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
    private static final String LOCALHOST_URL = "https://localhost:";
    public static final String NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE = "No HTTP endpoint has been defined in this " +
            "configuration. Not starting FA³ST service.";

    private final Monitor monitor;
    private final Map<String, Service> faaastServiceRepository;

    public FaaastServiceManager(Monitor monitor) {
        this.monitor = monitor;
        faaastServiceRepository = new ConcurrentHashMap<>();
    }

    @Override
    public URL startService(Path aasModelPath, int port) throws IOException {
        Objects.requireNonNull(aasModelPath);
        if (!isValidPort(port)) {
            throw new EdcException(format("Port is not valid: (%s).", port));
        }

        monitor.debug(format("Booting up FA³ST service using AAS model path (%s) and service port (%s).", aasModelPath,
                port));

        var serviceConfig = new ServiceConfig.Builder()
                .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                .persistence(PersistenceInMemoryConfig.builder().initialModelFile(aasModelPath.toFile()).build())
                .build();

        ServiceConfigHelper.autoComplete(serviceConfig);

        Service service;
        try {
            service = new Service(serviceConfig);
            service.start();
        } catch (Exception faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }

        monitor.debug("Booted up FA³ST service.");

        var faaastUrl = LOCALHOST_URL.concat(String.valueOf(port));

        faaastServiceRepository.put(faaastUrl, service);
        return new URL(faaastUrl);
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath) throws IOException {
        Objects.requireNonNull(aasModelPath);
        Objects.requireNonNull(configPath);
        return startService(aasModelPath, configPath, -1);
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath, int port) throws IOException {
        var localFaaastServicePort = 0;

        if (isValidPort(port)) {
            monitor.debug("Starting FA³ST service using model (%s), config (%s) and port (%s)."
                    .formatted(aasModelPath, configPath, port));
            localFaaastServicePort = port;
        } else {
            monitor.debug("Starting FA³ST service using model (%s) and config (%s)."
                    .formatted(aasModelPath, configPath));
        }

        try {
            var serviceConfig = ServiceConfigHelper.load(configPath.toFile());
            var isEndpointsNull = Objects.isNull(serviceConfig.getEndpoints()); // Remove auto generated httpEndpoint
            // later...

            if (localFaaastServicePort != 0) {
                if (isEndpointsNull) {
                    serviceConfig.setEndpoints(List.of(HttpEndpointConfig.builder().port(port).build()));
                } else {
                    var endpoints = serviceConfig.getEndpoints();
                    endpoints.add(HttpEndpointConfig.builder().port(port).build());
                    serviceConfig.setEndpoints(endpoints);
                }
            } else {
                var httpEndpointConfig = serviceConfig.getEndpoints().stream()
                        .filter(ep -> ep instanceof HttpEndpointConfig)
                        .findAny()
                        .orElseThrow(() -> new IllegalArgumentException(NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE));

                localFaaastServicePort = ((HttpEndpointConfig) httpEndpointConfig).getPort();
            }

            serviceConfig.setPersistence(PersistenceInMemoryConfig.builder().initialModelFile(aasModelPath.toFile()).build());
            ServiceConfigHelper.autoComplete(serviceConfig);

            // If localFaaastServicePort is unchanged, no valid HTTP Endpoint was found/created.
            if (localFaaastServicePort == 0) {
                throw new IllegalArgumentException(
                        NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE);
            }

            var service = new Service(serviceConfig);
            service.start();
            monitor.debug("Started FA³ST service.");

            faaastServiceRepository.put(LOCALHOST_URL.concat(String.valueOf(localFaaastServicePort)), service);

        } catch (MessageBusException | EndpointException | AssetConnectionException |
                 ConfigurationException faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return new URL(LOCALHOST_URL + localFaaastServicePort);
    }

    @Override
    public void stopServices() {
        monitor.debug("Shutting down all internally started FA³ST services...");
        faaastServiceRepository.values().forEach(Service::stop);
    }

    @Override
    public void stopService(URL aasServiceUrl) {
        Objects.requireNonNull(aasServiceUrl);
        monitor.debug(format("Shutting down FA³ST service with URL %s...", aasServiceUrl));

        var serviceToStop = faaastServiceRepository.get(aasServiceUrl.toString());
        if (Objects.nonNull(serviceToStop)) {
            serviceToStop.stop();
            faaastServiceRepository.remove(aasServiceUrl.toString());
        } else {
            monitor.debug("This URL was not registered as an internal FA³ST service.");
        }
    }

    private boolean isValidPort(int port) {
        return port < 65536 && port > 0;
    }

}
