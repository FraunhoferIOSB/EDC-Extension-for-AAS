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
package de.fraunhofer.iosb.model.aas.service;

import de.fraunhofer.iosb.model.aas.AasProvider;
import de.fraunhofer.iosb.model.aas.auth.AuthenticationMethod;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.net.URL;

/**
 * An AAS service representation as seen in <a href="https://github.com/FraunhoferIOSB/FAAAST-Service">FA³ST Service</a>
 */
public final class Service extends AasProvider {

    public static final String SHELLS_PATH = "shells";
    public static final String SUBMODELS_PATH = "submodels";
    public static final String CONCEPT_DESCRIPTIONS_PATH = "concept-descriptions";

    private Asset environment;

    /**
     * Create a new service with given access url and no environment and no required authentication.
     *
     * @param accessUrl URL for accessing the service.
     */
    public Service(@NotNull URL accessUrl) {
        super(new AasAccessUrl(accessUrl));
    }

    /**
     * Create a new service representation with given access url and empty environment and given authentication method.
     *
     * @param accessUrl            URL for accessing the service.
     * @param authenticationMethod The authentication method required to access this AAS service
     */
    public Service(URL accessUrl, AuthenticationMethod authenticationMethod) {
        super(new AasAccessUrl(accessUrl), authenticationMethod);
        this.environment = null;
    }

    /**
     * Create a new service from another object
     *
     * @param provider Provider to replicate.
     */
    public Service(AasProvider provider) {
        super(provider);
    }

    private Service(AasProvider provider, Asset environment) {
        super(provider);
        this.environment = environment;
    }

    /**
     * Return the current Service with the given environment.
     * This creates a new object reference.
     *
     * @param environment The environment
     * @return A new object containing this service's metadata and the environment.
     */
    public @NotNull Service with(Asset environment) {
        return new Service(this, environment);
    }

    public Asset environment() {
        return environment;
    }

    @Override
    public String toString() {
        return "Service[" +
                "accessUrl=" + getAccessUrl() + ", " +
                "environment=" + environment + ']';
    }
}
