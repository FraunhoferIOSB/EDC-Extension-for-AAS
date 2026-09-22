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
package de.fraunhofer.iosb.ilt.dataspace.model.context;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


/**
 * Context holding information about an AAS server.
 */
public abstract class AasServerContext {

    private final URI uri;
    private final List<PolicyBinding> policyBindings;
    private final boolean onlySubmodels;
    private final AuthenticationMethod authenticationMethod;
    private final boolean allowSelfSigned;


    /**
     * Creates a new AAS server context.
     *
     * @param uri the URI of the AAS server.
     * @param policyBindings policy bindings for this repository.
     * @param onlySubmodels whether only submodels should be registered.
     * @param authenticationMethod the authentication method for connecting to this AAS server.
     * @param allowSelfSigned whether to allow self-signed certificates when connecting to this AAS server.
     */
    protected AasServerContext(URI uri, List<PolicyBinding> policyBindings, boolean onlySubmodels, AuthenticationMethod authenticationMethod, boolean allowSelfSigned) {
        this.uri = uri;
        this.policyBindings = policyBindings;
        this.onlySubmodels = onlySubmodels;
        this.authenticationMethod = authenticationMethod;
        this.allowSelfSigned = allowSelfSigned;
    }


    /**
     * Returns whether authentication is required to connect to this AAS server.
     *
     * @return true if authentication is required, else false.
     */
    public boolean requiresAuthentication() {
        return !(authenticationMethod instanceof NoAuth);
    }


    /**
     * Returns the authentication method required to connect to this AAS server.
     *
     * @return The authentication method.
     */
    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    /**
     * Returns whether to allow self-signed certificates when connecting to this AAS server.
     *
     * @return Whether to allow self-signed certificates.
     */
    public boolean allowSelfSigned() {
        return allowSelfSigned;
    }


    /**
     * Returns the policy bindings for this AAS server.
     *
     * @return the policy bindings.
     */
    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }


    /**
     * Returns whether only submodels are to be registered.
     *
     * @return True if only submodels are to be registered, else false.
     */
    public boolean isOnlySubmodels() {
        return onlySubmodels;
    }


    /**
     * Returns the full URI to access this AAS repository.
     *
     * @return The full accessor URI for this repository.
     */
    public URI getUri() {
        return uri;
    }


    /**
     * Abstract builder for AAS server contexts.
     *
     * @param <T> the context type produced by this builder.
     * @param <B> the builder subtype for fluent chaining.
     */
    public abstract static class AbstractBuilder<T extends AasServerContext, B extends AbstractBuilder<T, B>> {
        /** URI of the AAS server. */
        protected URI uri;
        /** Policy bindings to set on the context. */
        protected List<PolicyBinding> policyBindings = new ArrayList<>();
        /** Whether only submodels should be registered. */
        protected boolean onlySubmodels;
        /** Authentication method for connecting to this AAS server. */
        protected AuthenticationMethod authenticationMethod = new NoAuth();
        /** Whether self-signed certificates are allowed. */
        protected boolean allowSelfSigned = false;


        /** Default constructor. */
        protected AbstractBuilder() {}


        /**
         * Returns this builder instance for fluent chaining.
         *
         * @return this builder.
         */
        protected abstract B self();


        /**
         * Sets the URI of the AAS server.
         *
         * @param uri the server URI.
         * @return this builder.
         */
        public B uri(URI uri) {
            this.uri = uri;
            return self();
        }


        /**
         * Sets the policy bindings.
         *
         * @param policyBindings policy bindings for this repository.
         * @return this builder.
         */
        public B policyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
            return self();
        }


        /**
         * Sets whether only submodels should be registered.
         *
         * @param onlySubmodels whether only submodels should be registered.
         * @return this builder.
         */
        public B onlySubmodels(boolean onlySubmodels) {
            this.onlySubmodels = onlySubmodels;
            return self();
        }


        /**
         * Sets the authentication method for connecting to this AAS server.
         *
         * @param authenticationMethod the authentication method.
         * @return this builder.
         */
        public B authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return self();
        }


        /**
         * Sets whether self-signed certificates are allowed.
         *
         * @param allowSelfSigned whether to allow self-signed certificates.
         * @return this builder.
         */
        public B allowSelfSigned(boolean allowSelfSigned) {
            this.allowSelfSigned = allowSelfSigned;
            return self();
        }


        /**
         * Validates and fills in default values for unset fields.
         */
        protected void validate() {
            Objects.requireNonNull(uri, "Access URI must be non-null");
        }

    }
}
