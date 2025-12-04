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
package de.fraunhofer.iosb.model.context.registry;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.model.context.AasServerContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.Objects;


public class AasRegistryContext extends AasServerContext {

    private final AuthenticationMethod authenticationMethod;
    private final boolean allowSelfSigned;


    private AasRegistryContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId, AuthenticationMethod authenticationMethod,
                               boolean allowSelfSigned) {
        super(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId);
        this.authenticationMethod = authenticationMethod;
        this.allowSelfSigned = allowSelfSigned;
    }


    /**
     * Returns the authentication method required to connect to this registry.
     *
     * @return The authentication method.
     */
    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    /**
     * Returns whether to allow self-signed certificates when connecting to this registry.
     *
     * @return Whether to allow self-signed certificates.
     */
    public boolean allowSelfSigned() {
        return allowSelfSigned;
    }


    @Override
    public boolean eligibleForRegistration(Reference reference) {
        return true;
    }


    public static class Builder extends AasServerContext.AbstractBuilder<AasRegistryContext, Builder> {
        private AuthenticationMethod authenticationMethod;
        private boolean allowSelfSigned;


        public Builder() {
        }


        public Builder authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }


        public Builder allowSelfSigned(boolean allowSelfSigned) {
            this.allowSelfSigned = allowSelfSigned;
            return this;
        }


        public AasRegistryContext build() {
            Objects.requireNonNull(uri, "Access URI must be non-null");
            authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());

            return new AasRegistryContext(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId, authenticationMethod, allowSelfSigned);
        }
    }
}
