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
package de.fraunhofer.iosb.api;

import de.fraunhofer.iosb.api.filter.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.api.model.Endpoint;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Let other extensions add public endpoints.
 */
public class PublicApiManagementService {

    private final CustomAuthenticationRequestFilter filter;
    private final Monitor monitor;

    public PublicApiManagementService(CustomAuthenticationRequestFilter filter, Monitor monitor) {
        this.filter = filter;
        this.monitor = monitor;
    }

    /**
     * Add a collection of public endpoints for the request filter to accept.
     *
     * @param endpoints Non-null collection of endpoints.
     */
    public void addEndpoints(Collection<Endpoint> endpoints) {
        Objects.requireNonNull(endpoints, "endpoints must not be null");
        monitor.info(format("PublicApiManagementService: Adding %s public endpoints to filter.", endpoints.size()));
        var nonNullEndpoints = endpoints.stream().filter(Objects::nonNull).collect(Collectors.toList());
        filter.addEndpoints(nonNullEndpoints);
    }

    /**
     * Add a temporary public endpoint for the request filter to accept.
     *
     * @param endpoint Non-null endpoint.
     */
    public void addTemporaryEndpoint(Endpoint endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        monitor.info(format("PublicApiManagementService: Adding public endpoint %s to filter", endpoint.suffix()));
        filter.addTemporaryEndpoint(endpoint);
    }

    /**
     * Remove a collection of public endpoints for the request filter to accept.
     *
     * @param endpoints Non-null collection of endpoints.
     */
    public void removeEndpoints(Collection<Endpoint> endpoints) {
        Objects.requireNonNull(endpoints, "endpoints must not be null");
        monitor.info(format("PublicApiManagementService: Removing %s public endpoints from filter.", endpoints.size()));
        filter.removeEndpoints(endpoints);
    }
}
