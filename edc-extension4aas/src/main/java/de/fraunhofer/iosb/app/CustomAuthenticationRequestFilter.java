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
package de.fraunhofer.iosb.app;

import java.util.Objects;

import org.eclipse.edc.api.auth.spi.AuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Custom AuthenticationRequestFilter filtering requests that go directly to an
 * AAS service (managed by this extension) or the extension's configuration.
 */
public class CustomAuthenticationRequestFilter extends AuthenticationRequestFilter {

    private final Configuration config;

    public CustomAuthenticationRequestFilter(AuthenticationService authenticationService) {
        super(authenticationService);
        config = Configuration.getInstance();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Objects.requireNonNull(requestContext);
        if (!(Endpoint.SELF_DESCRIPTION_PATH.equalsIgnoreCase(requestContext.getUriInfo().getPath())
                && config.isExposeSelfDescription())) {
            Logger.getInstance().debug("CustomAuthenticationRequestFilter: Intercepting this request");
            super.filter(requestContext);
        } else {
            Logger.getInstance().debug("CustomAuthenticationRequestFilter: Not intercepting this request");
        }
    }
}
