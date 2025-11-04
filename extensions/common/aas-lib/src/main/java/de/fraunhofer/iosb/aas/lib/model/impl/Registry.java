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
package de.fraunhofer.iosb.aas.lib.model.impl;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.net.AasAccessUri;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

/**
 * An AAS registry representation as seen in
 * <a href="https://github.com/fraunhoferIOSB/FAAAST-registry">FA³ST Registry</a>
 */
public final class Registry extends AasProvider {

    public static final String SUBMODEL_DESCRIPTORS_PATH = "submodel-descriptors";
    public static final String SHELL_DESCRIPTORS_PATH = "shell-descriptors";

    private Collection<Service> services;

    /**
     * Create a new AAS registry representation with given access uri and empty (nonnull) environment and no required
     * authentication method.
     *
     * @param accessUri URI for accessing the registry.
     */
    public Registry(URI accessUri) {
        super(new AasAccessUri(accessUri));
        this.services = new ArrayList<>();
    }

    /**
     * Create a new AAS registry representation with given access uri and environment and no required
     * authentication method.
     *
     * @param accessUri            URI for accessing the registry.
     * @param authenticationMethod The authentication needed by this registry.
     */
    public Registry(URI accessUri, AuthenticationMethod authenticationMethod) {
        super(new AasAccessUri(accessUri), authenticationMethod);
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

    /**
     * Returns services this registry holds. This can be null before synchronization happened
     *
     * @return The services this registry has registered.
     */
    public Collection<Service> services() {
        return services;
    }

    @Override
    public String toString() {
        return "Registry[" + "accessUri=" + super.baseUri() + ", " + "services=" + services + ']';
    }

}
