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
package de.fraunhofer.iosb.app.authentication;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.client.ClientEndpoint;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferEndpoint;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

/**
 * Custom AuthenticationRequestFilter filtering requests that go directly to an
 * AAS service (managed by this extension) or the extension's configuration.
 */
public class CustomAuthenticationRequestFilter extends AuthenticationRequestFilter {

    private static final Logger LOGGER = Logger.getInstance();
    private final Map<String, String> tempKeys;
    private final String[] endpoints;

    public CustomAuthenticationRequestFilter(AuthenticationService authenticationService, String... acceptedEndpoints) {
        super(authenticationService);
        tempKeys = new ConcurrentHashMap<>();
        if (Objects.nonNull(acceptedEndpoints)) {
            endpoints = acceptedEndpoints;
        } else {
            endpoints = new String[0];
        }
    }

    /**
     * Add key,value pair for a request. This key will only be available for one request.
     *
     * @param key   The key name
     * @param value The actual key
     */
    public void addTemporaryApiKey(String key, String value) {
        tempKeys.put(key, value);
    }

    /**
     * On automated data transfer: If the request is valid, the key,value pair used
     * for this request will no longer be valid.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        Objects.requireNonNull(requestContext);
        var requestPath = requestContext.getUriInfo().getPath();

        for (String endpoint : endpoints) {
            if (Objects.nonNull(endpoint) && endpoint.equalsIgnoreCase(requestPath)) {
                LOGGER.debug(
                        "CustomAuthenticationRequestFilter: Not intercepting this request to an open endpoint");
                return;
            }
        }

        for (String key : tempKeys.keySet()) {
            if (requestContext.getHeaders().containsKey(key)
                    && requestContext.getHeaderString(key).equals(tempKeys.get(key))
                    && requestPath.startsWith(
                    format("%s/%s", ClientEndpoint.AUTOMATED_PATH, DataTransferEndpoint.RECEIVE_DATA_PATH))) {
                LOGGER.debug(
                        format("CustomAuthenticationRequestFilter: Data Transfer request with custom api key %s", key));
                tempKeys.remove(key);
                return;
            }
        }

        LOGGER.debug("CustomAuthenticationRequestFilter: Intercepting this request");
        super.filter(requestContext);
    }
}
