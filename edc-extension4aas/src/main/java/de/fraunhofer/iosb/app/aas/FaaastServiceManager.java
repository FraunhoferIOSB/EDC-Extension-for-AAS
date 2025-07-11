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

import de.fraunhofer.iosb.aas.lib.net.AasAccessUrl;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static de.fraunhofer.iosb.app.util.OperatingSystemUtil.getLocalhostAddress;

/**
 * Manages internally created FA³ST instances.
 */
public class FaaastServiceManager implements AssetAdministrationShellServiceManager {

    public static final String NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE = "No valid HTTP endpoint could be defined in " + "this configuration.";
    private static final String FAAAST = "FA³ST";
    private static final String GENERIC_EXCEPTION_MESSAGE = "Exception thrown by %s service.".formatted(FAAAST);
    private static final String LOCALHOST_URL = getLocalhostAddress();

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

        var httpEndpoints = serviceConfig.getEndpoints().stream()
                .filter(endpoint -> endpoint instanceof HttpEndpointConfig)
                .map(endpoint -> (HttpEndpointConfig) endpoint)
                .toList();

        if (httpEndpoints.stream().map(HttpEndpointConfig::getPort).noneMatch(p -> port == p || isValidPort(p) && available(p))) {
            throw new IllegalArgumentException(NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE);
        }

        // If port was explicitly provided, take this port for communication
        var communicationHttpPort = (httpEndpoints.stream().anyMatch(endpoint -> endpoint.getPort() == port) ?
                httpEndpoints.stream().filter(endpoint -> endpoint.getPort() == port).findFirst() :
                httpEndpoints.stream().findAny())
                .orElseThrow(() -> new IllegalArgumentException(NO_ENDPOINT_DEFINED_EXCEPTION_MESSAGE));

        var urlSpec =
                "http".concat(communicationHttpPort.isSslEnabled() ? "s" : "") // proto
                        .concat("://")
                        .concat(Optional.ofNullable(communicationHttpPort.getHostname()).orElse(LOCALHOST_URL)) // hostname
                        .concat(":")
                        .concat(String.valueOf(communicationHttpPort.getPort())) // port
                        .concat("/api/v3.0"); // path

        var serviceAccessUrl = new URL(urlSpec);
        faaastServiceRepository.put(new AasAccessUrl(serviceAccessUrl), service);
        monitor.debug("Started %s service with access URL: %s.".formatted(FAAAST, serviceAccessUrl));

        return serviceAccessUrl;
    }

    @Override
    public void stopServices() {
        faaastServiceRepository.values().forEach(Service::stop);
        monitor.info("Stopped all internally started %s services...".formatted(FAAAST));
    }

    @Override
    public void stopService(URL aasServiceUrl) {
        Objects.requireNonNull(aasServiceUrl);
        monitor.debug("Shutting down %s service with URL %s...".formatted(FAAAST, aasServiceUrl));

        var serviceToStop = faaastServiceRepository.get(new AasAccessUrl(aasServiceUrl));
        if (Objects.nonNull(serviceToStop)) {
            serviceToStop.stop();
            faaastServiceRepository.remove(new AasAccessUrl(aasServiceUrl));
        } else {
            monitor.debug("This URL was not registered as an internal %s service.".formatted(FAAAST));
        }
    }

    private @NotNull Service createAndStartService(ServiceConfig serviceConfig) {
        Service service;
        try {
            service = new Service(serviceConfig);
        } catch (AssetConnectionException | ConfigurationException faaastServiceException) {
            throw new EdcException(GENERIC_EXCEPTION_MESSAGE, faaastServiceException);
        }

        try {
            service.start();
        } catch (Exception faaastServiceException) {
            throw new EdcException(GENERIC_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return service;
    }

    private @NotNull ServiceConfig getConfig(Path aasModelPath, Path configPath, int port) throws IOException {
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

    private boolean available(int port) {
        try (var socket = new Socket(LOCALHOST_URL, port)) {
            socket.setReuseAddress(true);
            // Connected to some service -> unavailable
            return false;
        } catch (IOException ignored) {
            var ignoredBody = "";
        }

        try (var serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
        } catch (IOException ignored) {
            return false;
        }

        try (var datagramSocket = new DatagramSocket(port)) {
            datagramSocket.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isValidPort(int port) {
        return port < 65536 && port > 0;
    }

}
