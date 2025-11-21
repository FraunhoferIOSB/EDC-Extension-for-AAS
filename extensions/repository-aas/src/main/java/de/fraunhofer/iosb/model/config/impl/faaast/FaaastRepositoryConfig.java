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


public class FaaastRepositoryConfig extends AasRepositoryConfig<ServiceConfig> {

    private final Integer customPort;
    private final ServiceConfig serviceConfig;


    private FaaastRepositoryConfig(Integer customPort, ServiceConfig serviceConfig, List<PolicyBinding> policyBindings) {
        super(policyBindings);
        this.customPort = customPort;
        this.serviceConfig = serviceConfig;
    }


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
                .filter(ep -> ep.getPort() != customPort)
                .findFirst()
                .orElseGet(() -> httpEndpoints.get(0));
    }


    @Override
    public ServiceConfig get() {
        return this.serviceConfig;
    }


    public static class Builder extends AasRepositoryConfig.Builder<Builder, FaaastRepositoryConfig> {
        private Integer port = null;
        private Path configPath;
        private boolean ssl = true;
        private boolean sni = true;


        private Builder() {
        }


        public static Builder newInstance() {
            return new Builder();
        }


        @Override
        public Builder self() {
            return this;
        }


        public Builder port(Integer port) {
            this.port = port;
            return this;
        }


        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }


        public Builder configPath(Path configPath) {
            this.configPath = configPath;
            return this;
        }


        public Builder configPath(String configPath) {
            this.configPath = Optional.ofNullable(configPath)
                    .map(Path::of)
                    .orElse(null);

            return this;
        }


        public FaaastRepositoryConfig build() {
            ServiceConfig serviceConfig =
                    Optional.ofNullable(configPath)
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
                                    .sni(sni)
                                    .build()));

            ServiceConfigHelper.autoComplete(serviceConfig);

            // A FA³ST service can be built with no model and default configuration.
            return new FaaastRepositoryConfig(port, serviceConfig, policyBindings);
        }
    }
}
