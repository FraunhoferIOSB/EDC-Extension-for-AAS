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
package de.fraunhofer.iosb.model.config.impl.faaast;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import de.fraunhofer.iosb.model.config.AasRepositoryConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


/** Configuration for a FA³ST-based AAS repository. */
public class FaaastRepositoryConfig extends AasRepositoryConfig<ServiceConfig> {

    /** Custom port for the FA³ST service, or null if none is set. */
    private final Integer customPort;
    /** The underlying FA³ST service configuration. */
    private final ServiceConfig serviceConfig;


    /**
     * Creates a new FA³ST repository configuration.
     *
     * @param customPort custom port for the FA³ST service, or null.
     * @param serviceConfig the FA³ST service configuration.
     * @param policyBindings policy bindings for this repository.
     * @param onlySubmodels whether only submodels should be registered.
     */
    private FaaastRepositoryConfig(Integer customPort, ServiceConfig serviceConfig, List<PolicyBinding> policyBindings, boolean onlySubmodels) {
        super(policyBindings, onlySubmodels);
        this.customPort = customPort;
        this.serviceConfig = serviceConfig;
    }


    /**
     * Returns the HTTP endpoint configuration for the FA³ST service.
     *
     * @return the HTTP endpoint configuration.
     */
    public HttpEndpointConfig getPort() {
        List<HttpEndpointConfig> httpEndpoints = serviceConfig.getEndpoints().stream()
                .filter(HttpEndpointConfig.class::isInstance)
                .map(ep -> (HttpEndpointConfig) ep)
                .toList();

        if (httpEndpoints.isEmpty()) {
            throw new IllegalStateException("FA³ST service does not expose any endpoint.");
        }

        // Find any pre-defined endpoint to return or else return the possibly custom EDC-communication port.
        return httpEndpoints.stream()
                .filter(ep -> customPort == null || (ep.getPort() != customPort))
                .findFirst()
                .orElseGet(() -> httpEndpoints.get(0));
    }


    /**
     * Returns the FA³ST service configuration.
     *
     * @return the FA³ST service configuration.
     */
    @Override
    public ServiceConfig get() {
        return this.serviceConfig;
    }


    /**
     * Builder for {@link FaaastRepositoryConfig}.
     */
    public static class Builder extends AasRepositoryConfig.Builder<Builder, FaaastRepositoryConfig> {
        private Integer port = null;
        private Path configPath;
        private boolean ssl = true;


        /** Default constructor. */
        private Builder() {}


        /**
         * Creates a new builder instance.
         *
         * @return a new builder.
         */
        public static Builder newInstance() {
            return new Builder();
        }


        /**
         * Returns this builder instance for fluent chaining.
         *
         * @return this builder.
         */
        @Override
        public Builder self() {
            return this;
        }


        /**
         * Sets the port for the FA³ST service.
         *
         * @param port the port number.
         * @return this builder.
         */
        public Builder port(Integer port) {
            this.port = port;
            return this;
        }


        /**
         * Sets whether SSL is enabled for the FA³ST service.
         *
         * @param ssl whether SSL is enabled.
         * @return this builder.
         */
        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }


        /**
         * Sets the path to the FA³ST configuration file.
         *
         * @param configPath path to the configuration file.
         * @return this builder.
         */
        public Builder configPath(Path configPath) {
            this.configPath = configPath;
            return this;
        }


        /**
         * Sets the path to the FA³ST configuration file from a string.
         *
         * @param configPath path to the configuration file as string.
         * @return this builder.
         */
        public Builder configPath(String configPath) {
            this.configPath = Optional.ofNullable(configPath)
                    .map(Path::of)
                    .orElse(null);

            return this;
        }


        /**
         * Builds the {@link FaaastRepositoryConfig} instance.
         *
         * @return the built configuration.
         */
        public FaaastRepositoryConfig build() {
            ServiceConfig serviceConfig = Optional.ofNullable(configPath)
                    .map(Path::toFile)
                    .map(file -> {
                        try {
                            return ServiceConfigHelper.load(file);
                        }
                        catch (IOException e) {
                            throw new IllegalArgumentException(String.format("Could not load FA³ST service config from path %s",
                                    configPath), e);
                        }
                    })
                    .orElse(new ServiceConfig());

            // Any additional given argument supersedes or complements the values that are present in the config
            Optional.ofNullable(model)
                    .ifPresent(model -> {
                        var persistence = Optional.ofNullable(serviceConfig.getPersistence())
                                .orElse(new PersistenceInMemoryConfig());

                        persistence.setInitialModelFile(model.toFile());
                        serviceConfig.setPersistence(persistence);
                    });

            Optional.ofNullable(port)
                    .ifPresent(p -> serviceConfig.getEndpoints()
                            .add(HttpEndpointConfig.builder()
                                    .port(p)
                                    .ssl(ssl)
                                    .build()));

            ServiceConfigHelper.autoComplete(serviceConfig);

            // A FA³ST service can be built with no model and default configuration.
            return new FaaastRepositoryConfig(port, serviceConfig, policyBindings, onlySubmodels);
        }
    }
}
