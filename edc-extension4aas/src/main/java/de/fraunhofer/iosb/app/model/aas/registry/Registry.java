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
package de.fraunhofer.iosb.app.model.aas.registry;

import de.fraunhofer.iosb.app.model.aas.service.Service;
import de.fraunhofer.iosb.model.aas.AasProvider;
import de.fraunhofer.iosb.model.aas.auth.AuthenticationMethod;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An AAS registry representation as seen in
 * <a href="https://github.com/fraunhoferIOSB/FAAAST-registry">FA³ST Registry</a>
 */
public final class Registry extends AasProvider {

    public static final String SUBMODEL_DESCRIPTORS_PATH = "%s/submodel-descriptors".formatted(AAS_V3_PREFIX);
    public static final String SHELL_DESCRIPTORS_PATH = "%s/shell-descriptors".formatted(AAS_V3_PREFIX);

    @Nullable
    private Collection<Service> services;

    /**
     * Create a new AAS registry representation with given access url and empty (nonnull) environment and no required
     * authentication method.
     *
     * @param accessUrl URL for accessing the registry.
     */
    public Registry(@Nonnull URL accessUrl) {
        super(new AasAccessUrl(accessUrl));
        this.services = new ArrayList<>();
    }

    /**
     * Create a new AAS registry representation with given access url and environment and no required
     * authentication method.
     *
     * @param accessUrl            URL for accessing the registry.
     * @param authenticationMethod The authentication needed by this registry.
     */
    public Registry(@Nonnull URL accessUrl, @Nonnull AuthenticationMethod authenticationMethod) {
        super(new AasAccessUrl(accessUrl), authenticationMethod);
    }

    /**
     * Get this registry with the given services
     *
     * @param services Services to be associated with the registry
     * @return The updated registry.
     */
    public Registry with(Collection<Service> services) {
        this.services = services;
        return this;
    }

    public URL getSubmodelDescriptorUrl() throws MalformedURLException {
        return new URL(getAccessUrl(), SUBMODEL_DESCRIPTORS_PATH);
    }

    public URL getShellDescriptorUrl() throws MalformedURLException {
        return new URL(getAccessUrl(), SHELL_DESCRIPTORS_PATH);
    }

    /**
     * Returns services this registry holds. This can be null before synchronization happened
     *
     * @return The services this registry has registered.
     */
    @Nullable
    public Collection<Service> services() {
        return services;
    }

    @Override
    public String toString() {
        return "Registry[" +
                "accessUrl=" + super.getAccessUrl() + ", " +
                "services=" + services + ']';
    }

}
