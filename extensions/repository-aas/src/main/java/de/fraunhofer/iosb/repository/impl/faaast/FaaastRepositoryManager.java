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
package de.fraunhofer.iosb.repository.impl.faaast;

import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ConfigurationException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.model.config.impl.faaast.FaaastRepositoryConfig;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import de.fraunhofer.iosb.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import de.fraunhofer.iosb.repository.AasRepositoryManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.Hostname;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manages internally created FA³ST instances.
 */
public class FaaastRepositoryManager implements AasRepositoryManager<FaaastRepositoryConfig> {
    public static final String FAAAST = "FA³ST";
    private static final String GENERIC_EXCEPTION_MESSAGE = "Exception thrown by %s service.".formatted(FAAAST);

    private final Hostname hostname;
    private final Monitor monitor;
    private final Map<URI, Service> repository;


    public FaaastRepositoryManager(Monitor monitor, Hostname hostname) {
        this.monitor = monitor;
        this.hostname = hostname;

        this.repository = new ConcurrentHashMap<>();
    }


    @Override
    public AasRepositoryContext startRepository(FaaastRepositoryConfig config) {
        ServiceConfig serviceConfig = config.get();

        Service service = createAndStartService(serviceConfig);

        HttpEndpointConfig endpointConfig = config.getPort();

        URI accessUri = URI.create(
                "http".concat(endpointConfig.isSslEnabled() ? "s" : "") // proto
                        .concat("://")
                        .concat(Optional.ofNullable(endpointConfig.getHostname()).orElse(hostname.get())) // hostname
                        .concat(":")
                        .concat(String.valueOf(endpointConfig.getPort())) // port
                        .concat("/api/v3.0")); // mandatory FA³ST path

        repository.put(accessUri, service);
        monitor.debug("Started %s service with access URL: %s.".formatted(FAAAST, accessUri));

        return new LocalFaaastRepositoryContext.Builder()
                .uri(accessUri)
                .messageBus(service.getMessageBus())
                .persistence(service.getPersistence())
                .build();
    }


    @Override
    public void stopAll() {
        repository.values().forEach(Service::stop);
        monitor.info("Stopped all internally started %s services.".formatted(FAAAST));
    }


    @Override
    public void stopRepository(URI repositoryUri) {
        Objects.requireNonNull(repositoryUri);
        monitor.info("Stopping internal %s service with URI %s.".formatted(FAAAST, repositoryUri));

        var serviceToStop = repository.remove(repositoryUri);
        if (Objects.isNull(serviceToStop)) {
            monitor.info("%s was not registered as an internal %s service.".formatted(repositoryUri, FAAAST));
            return;
        }

        asyncStopService(repositoryUri, serviceToStop);
    }


    private void asyncStopService(URI repositoryUri, Service serviceToStop) {
        CompletableFuture.runAsync(() -> {
            try {
                serviceToStop.stop();
            }
            catch (Exception e) {
                monitor.warning(String.format("Could not stop internal %s service with URI %s", FAAAST, repositoryUri), e);
            }
        });
    }


    private @NotNull Service createAndStartService(ServiceConfig serviceConfig) {
        Service service;
        try {
            service = new Service(serviceConfig);
            service.start();
        }
        catch (AssetConnectionException | ConfigurationException | PersistenceException | MessageBusException |
                EndpointException faaastServiceException) {
            throw new EdcException(GENERIC_EXCEPTION_MESSAGE, faaastServiceException);
        }
        return service;
    }
}
