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
import org.eclipse.edc.api.auth.spi.registry.ApiAuthenticationRegistry;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provides;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;


/**
 * Manage public api endpoints in a unified extension. This is due to multiple independent authentication request filters not working properly, since they cannot "let a request
 * through" to their endpoints without other registered authentication request filters accepting the request too.
 */
@Provides(PublicApiManagementService.class)
@Extension(value = PublicApiManagementExtension.NAME)
public class PublicApiManagementExtension implements ServiceExtension {

    public static final String NAME = "Public API Endpoint Management";

    // Our authentication request filter needs this service to work:
    @Inject
    private ApiAuthenticationRegistry apiAuthenticationRegistry;
    // To register our authentication request filter, we need:
    @Inject
    private WebService webService;


    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var filter = new CustomAuthenticationRequestFilter(apiAuthenticationRegistry, monitor);

        // Register our filter at the EDC
        webService.registerResource(filter);

        // Register this service to be accessible by other extensions via injection
        context.registerService(PublicApiManagementService.class, new PublicApiManagementService(filter, monitor));
    }
}
