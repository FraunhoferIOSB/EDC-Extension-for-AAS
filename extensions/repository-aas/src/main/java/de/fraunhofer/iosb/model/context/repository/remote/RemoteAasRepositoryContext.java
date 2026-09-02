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
package de.fraunhofer.iosb.model.context.repository.remote;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Objects;


/**
 * Context holding information about an AAS registry.
 */
public class RemoteAasRepositoryContext extends AasRepositoryContext {

    /** Error message template used when remote repository operations fail. */
    public static final String ERR_MSG_TEMPLATE = "%s from %s failed.";

    private final AuthenticationMethod authenticationMethod;
    private final boolean allowSelfSigned;


    private RemoteAasRepositoryContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId, List<PolicyBinding> policyBindings,
                                       AuthenticationMethod authenticationMethod, boolean allowSelfSigned, boolean onlySubmodels) {
        super(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId, policyBindings, onlySubmodels);
        this.authenticationMethod = authenticationMethod;
        this.allowSelfSigned = allowSelfSigned;
    }


    /**
     * Returns whether to allow connections to this repository if it holds self-signed certificates.
     *
     * @return True if allowing connections to this repository if holding self-signed certificates, else false.
     */
    public boolean allowSelfSigned() {
        return allowSelfSigned;
    }


    /**
     * Returns the authentication method used to connect to this repository.
     *
     * @return the authentication method.
     */
    public @NotNull AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    /**
     * Builder for {@link RemoteAasRepositoryContext}.
     */
    public static class Builder extends AbstractBuilder<RemoteAasRepositoryContext, Builder> {
        private AuthenticationMethod authenticationMethod;
        private boolean allowSelfSigned;


        /** Default constructor. */
        public Builder() {}


        /**
         * Sets the authentication method for connecting to this repository.
         *
         * @param authenticationMethod the authentication method.
         * @return this builder.
         */
        public Builder authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }


        /**
         * Sets whether self-signed certificates are allowed.
         *
         * @param allowSelfSigned whether to allow self-signed certificates.
         * @return this builder.
         */
        public Builder allowSelfSigned(boolean allowSelfSigned) {
            this.allowSelfSigned = allowSelfSigned;
            return this;
        }


        /**
         * Builds the {@link RemoteAasRepositoryContext} instance.
         *
         * @return the built context.
         */
        public RemoteAasRepositoryContext build() {
            super.validate();
            this.authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());

            return new RemoteAasRepositoryContext(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId, policyBindings, authenticationMethod, allowSelfSigned,
                    onlySubmodels);
        }
    }
}
