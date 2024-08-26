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
package de.fraunhofer.iosb.app.model.aas.service;

import de.fraunhofer.iosb.model.aas.AasProvider;
import de.fraunhofer.iosb.model.aas.auth.AuthenticationMethod;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nullable;

/**
 * An AAS service representation as seen in <a href="https://github.com/FraunhoferIOSB/FAAAST-Service">FA³ST Service</a>
 */
public final class Service extends AasProvider {

    public static final String SHELLS_PATH = "%s/shells".formatted(AAS_V3_PREFIX);
    public static final String SUBMODELS_PATH = "%s/submodels".formatted(AAS_V3_PREFIX);
    public static final String CONCEPT_DESCRIPTIONS_PATH = "%s/concept-descriptions".formatted(AAS_V3_PREFIX);

    @Nullable
    private Asset environment;

    /**
     * Create a new service with given access url and no environment and no required authentication.
     *
     * @param accessUrl URL for accessing the service.
     */
    public Service(URL accessUrl) {
        super(new AasAccessUrl(accessUrl));
        this.environment = null;
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

    public @NotNull Service with(Asset environment) {
        this.environment = environment;
        return this;
    }

    public Asset environment() {
        return environment;
    }

    @Override
    public String toString() {
        return "Service[" +
                "accessUrl=" + super.getAccessUrl() + ", " +
                "environment=" + environment + ']';
    }

    public URL getShellsUrl() throws MalformedURLException {
        return new URL(getAccessUrl(), SHELLS_PATH);
    }

    public URL getSubmodelsUrl() throws MalformedURLException {
        return new URL(getAccessUrl(), SUBMODELS_PATH);
    }

    public URL getConceptDescriptionsUrl() throws MalformedURLException {
        return new URL(getAccessUrl(), CONCEPT_DESCRIPTIONS_PATH);
    }
}
