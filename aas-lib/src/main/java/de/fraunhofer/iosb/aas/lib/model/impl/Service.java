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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.model.AasProvider;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.net.AasAccessUrl;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * An AAS service representation as seen in <a href="https://github.com/FraunhoferIOSB/FAAAST-Service">FA³ST Service</a>
 */
@JsonDeserialize(builder = Service.Builder.class)
public final class Service extends AasProvider {

    public static final String SHELLS_PATH = "shells";
    public static final String SUBMODELS_PATH = "submodels";
    public static final String CONCEPT_DESCRIPTIONS_PATH = "concept-descriptions";

    private final Asset environment;
    private final List<PolicyBinding> policyBindings;

    /**
     * Create a new service representation with given access url and empty environment and given authentication method.
     *
     * @param accessUrl            URL for accessing the service.
     * @param authenticationMethod The authentication method required to access this AAS service
     */
    private Service(AasAccessUrl accessUrl, AuthenticationMethod authenticationMethod, Asset environment, List<PolicyBinding> policyBindings) {
        super(accessUrl, authenticationMethod);
        this.environment = environment;
        this.policyBindings = policyBindings;
    }

    /**
     * Attach an environment to a service.
     * This creates a new object reference.
     *
     * @param environment The environment
     * @return A new object containing this service's metadata and the environment.
     */
    public @NotNull Service with(Asset environment) {
        return this.toBuilder().environment(environment).build();
    }

    public Asset getEnvironment() {
        return environment;
    }


    /**
     * Returns whether this service is equipped with AAS-entity-level selection of elements to register and attached policies
     *
     * @return true if selective registration is wanted
     */
    public boolean hasSelectiveRegistration() {
        return null != policyBindings && !policyBindings.isEmpty();
    }

    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }

    @Override
    public String toString() {
        return "Service[" +
                "accessUrl=" + baseUrl() + ", " +
                "environment=" + environment + ']';
    }

    public Builder toBuilder() {
        return new Builder()
                .aasAccessUrl(this.url)
                .withAuth(this.auth)
                .withPolicyBindings(this.policyBindings)
                .environment(this.environment);
    }

    public static class Builder extends AasProvider.Builder<Service.Builder> {

        private Asset environment;

        public Builder environment(Asset environment) {
            this.environment = environment;
            return this;
        }

        public Service build() {
            return new Service(url, authentication, environment, policyBindings);
        }
    }
}
