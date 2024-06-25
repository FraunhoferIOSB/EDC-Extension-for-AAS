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
package de.fraunhofer.iosb.api.filter;

import de.fraunhofer.iosb.api.model.Endpoint;
import de.fraunhofer.iosb.api.model.HttpMethod;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Custom AuthenticationRequestFilter filtering requests that go directly to public endpoints.
 * Endpoints can be made public by adding them to this filter's list.
 */
public class CustomAuthenticationRequestFilter extends AuthenticationRequestFilter {

    private final Monitor monitor;
    private final Collection<Endpoint> endpoints;
    private final Collection<Endpoint> temporaryEndpoints;

    public CustomAuthenticationRequestFilter(AuthenticationService authenticationService, Monitor monitor) {
        super(authenticationService);
        this.monitor = monitor;
        endpoints = new ArrayList<>();
        temporaryEndpoints = new ArrayList<>();
    }

    /**
     * On automated data transfer: If the request is valid, the key,value pair used
     * for this request will no longer be valid.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Objects.requireNonNull(requestContext);
        var requestedEndpoint = parseEndpoint(requestContext);
        for (Endpoint endpoint : temporaryEndpoints) {
            if (endpoint.isCoveredBy(requestedEndpoint)) {
                monitor.debug(format("CustomAuthenticationRequestFilter: Accepting request to open temporary endpoint %s", endpoint.suffix()));
                temporaryEndpoints.remove(endpoint);
                return;
            }
        }

        for (Endpoint endpoint : endpoints) {
            if (endpoint.isCoveredBy(requestedEndpoint)) {
                monitor.debug(format("CustomAuthenticationRequestFilter: Accepting request to open endpoint %s", endpoint.suffix()));
                return;
            }
        }
        super.filter(requestContext);
    }

    private Endpoint parseEndpoint(ContainerRequestContext requestContext) {
        var requestPath = requestContext.getUriInfo().getPath();
        var method = HttpMethod.valueOf(requestContext.getMethod());
        var headers = requestContext.getHeaders().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        return new Endpoint(requestPath, method, headers);
    }

    public boolean addEndpoints(Collection<Endpoint> endpoints) {
        var newEndpoints = endpoints.stream().filter(newEndpoint -> !this.endpoints.contains(newEndpoint)).toList();
        return this.endpoints.addAll(newEndpoints);
    }

    public boolean removeEndpoints(Collection<Endpoint> endpoints) {
        return this.endpoints.removeAll(endpoints);
    }

    public void addTemporaryEndpoint(Endpoint endpoint) {
        temporaryEndpoints.add(endpoint);
    }
}
