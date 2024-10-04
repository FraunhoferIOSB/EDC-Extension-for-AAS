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

/**
 * Extensions can register their public endpoints here. Requests to those endpoints will not need authentication.
 * If multiple extensions handle their own public endpoints with separate AuthenticationRequestFilters, they will
 * interfere with each other.
 */
public class PublicApiManagementService {

    private final CustomAuthenticationRequestFilter filter;
    private final Monitor monitor;

    public PublicApiManagementService(CustomAuthenticationRequestFilter filter, Monitor monitor) {
        this.filter = filter;
        this.monitor = monitor.withPrefix("PublicApiManagementService");
    }

    /**
     * Add a collection of public endpoints for the request filter to accept.
     *
     * @param endpoints Non-null collection of endpoints.
     */
    public void addEndpoints(Collection<Endpoint> endpoints) {
        if (endpoints == null) {
            throw new NullPointerException("endpoints must not be null");
        }

        monitor.debug("Adding %s public endpoints.".formatted(endpoints.size()));

        endpoints.removeIf(Objects::isNull);
        filter.addEndpoints(endpoints);
    }

    /**
     * Add a temporary public endpoint for the request filter to accept.
     *
     * @param endpoint Non-null endpoint.
     */
    public void addTemporaryEndpoint(Endpoint endpoint) {
        if (endpoint == null) {
            throw new NullPointerException("endpoint must not be null");
        }

        monitor.debug("Adding public endpoint %s.".formatted(endpoint.suffix()));
        filter.addTemporaryEndpoint(endpoint);
    }

    /**
     * Remove a collection of public endpoints for the request filter to accept.
     *
     * @param endpoints Non-null collection of endpoints.
     */
    public void removeEndpoints(Collection<Endpoint> endpoints) {
        if (endpoints == null) {
            throw new NullPointerException("endpoints must not be null");
        }

        monitor.debug("Removing %s public endpoints.".formatted(endpoints.size()));
        endpoints.removeIf(Objects::isNull);
        filter.removeEndpoints(endpoints);
    }
}
