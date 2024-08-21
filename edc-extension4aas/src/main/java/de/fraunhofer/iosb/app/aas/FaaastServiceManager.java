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

import de.fraunhofer.iosb.app.model.aas.AasAccessUrl;
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static de.fraunhofer.iosb.ilt.faaast.service.util.HostnameUtil.LOCALHOST;

/**
 * Manages internally created FA³ST instances.
 */
public class FaaastServiceManager implements AssetAdministrationShellServiceManager {

    private static final String FAST = "FA³ST";
    private static final String FAAAST_SERVICE_EXCEPTION_MESSAGE = "Exception thrown by %s service.".formatted(FAST);
    public static final String NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE = "No valid HTTP endpoint could be defined in this configuration.";

    private final Monitor monitor;
    private final Map<AasAccessUrl, Service> faaastServiceRepository;

    public FaaastServiceManager(Monitor monitor) {
        this.monitor = monitor;
        faaastServiceRepository = new ConcurrentHashMap<>();
    }

    @Override
    public URL startService(Path aasModelPath, int port) throws IOException {
        Objects.requireNonNull(aasModelPath);
        return startService(aasModelPath, null, port);

    }

    @Override
    public URL startService(Path aasModelPath, Path configPath) throws IOException {
        Objects.requireNonNull(aasModelPath);
        Objects.requireNonNull(configPath);
        return startService(aasModelPath, configPath, -1);
    }

    @Override
    public URL startService(Path aasModelPath, Path configPath, int port) throws IOException {
        var serviceConfig = getConfig(aasModelPath, configPath, port);

        var service = createAndStartService(serviceConfig);

        var httpEndpoint = (HttpEndpointConfig) serviceConfig.getEndpoints().stream()
                .filter(e -> e instanceof HttpEndpointConfig)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE));

        var urlSpec = "http"
                .concat(httpEndpoint.isSslEnabled() ? "s" : "")
                .concat("://")
                .concat(Optional.ofNullable(httpEndpoint.getHostname()).orElse(LOCALHOST))
                .concat(":")
                .concat(String.valueOf(httpEndpoint.getPort()));

        var serviceAccessUrl = new URL(urlSpec);
        faaastServiceRepository.put(new AasAccessUrl(serviceAccessUrl), service);
        monitor.debug("Started %s service with access URL: %s.".formatted(FAST, serviceAccessUrl));

        return serviceAccessUrl;
    }

    @Override
    public void stopServices() {
        faaastServiceRepository.values().forEach(Service::stop);
        monitor.info("Stopped all internally started %s services...".formatted(FAST));
    }

    @Override
    public void stopService(URL aasServiceUrl) {
        Objects.requireNonNull(aasServiceUrl);
        monitor.debug("Shutting down %s service with URL %s...".formatted(FAST, aasServiceUrl));

        var serviceToStop = faaastServiceRepository.get(new AasAccessUrl(aasServiceUrl));
        if (Objects.nonNull(serviceToStop)) {
            serviceToStop.stop();
            faaastServiceRepository.remove(new AasAccessUrl(aasServiceUrl));
        } else {
            monitor.debug("This URL was not registered as an internal %s service.".formatted(FAST));
        }
    }

    private static @NotNull Service createAndStartService(ServiceConfig serviceConfig) {
        Service service;
        try {
            service = new Service(serviceConfig);
        } catch (AssetConnectionException | ConfigurationException faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }

        try {
            service.start();
        } catch (MessageBusException | EndpointException faaastServiceException) {
            throw new EdcException(FAAAST_SERVICE_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return service;
    }

    private ServiceConfig getConfig(Path aasModelPath, Path configPath, int port) throws IOException {
        var serviceConfig = configPath == null ?
                new ServiceConfig() :
                ServiceConfigHelper.load(configPath.toFile());

        if (isValidPort(port) && available(port)) {
            var endpoints = Optional.ofNullable(serviceConfig.getEndpoints()).orElse(new ArrayList<>());

            endpoints.add(HttpEndpointConfig.builder().port(port).build());

            serviceConfig.setEndpoints(endpoints);
        }

        // Set model
        serviceConfig.setPersistence(PersistenceInMemoryConfig.builder().initialModelFile(aasModelPath.toFile()).build());

        // Autocomplete: If undefined, add HTTPS endpoint (port 443)
        ServiceConfigHelper.autoComplete(serviceConfig);
        return serviceConfig;
    }

    private boolean available(int port) throws IllegalStateException {
        try (Socket ignored = new Socket("localhost", port)) {
            // Connected to some service -> unavailable
            monitor.info("Port %s is unavailable.".formatted(port));
            return false;
        } catch (ConnectException e) {
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Error while trying to check open port", e);
        }
    }

    private boolean isValidPort(int port) {
        return port < 65536 && port > 0;
    }

}
