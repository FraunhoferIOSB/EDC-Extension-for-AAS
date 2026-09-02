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
package de.fraunhofer.iosb.model.context;

import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.Objects;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_ACCESS_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_USAGE_POLICY_DEFINITION_ID;


/**
 * Context holding information about an AAS server.
 */
public abstract class AasServerContext {

    /** Default access policy definition ID used when none is explicitly configured. */
    protected final String defaultAccessPolicyDefinitionId;
    /** Default contract policy definition ID used when none is explicitly configured. */
    protected final String defaultContractPolicyDefinitionId;
    private final URI uri;


    /**
     * Creates a new AAS server context.
     *
     * @param uri the URI of the AAS server.
     * @param defaultAccessPolicyDefinitionId the default access policy definition ID.
     * @param defaultContractPolicyDefinitionId the default contract policy definition ID.
     */
    protected AasServerContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId) {
        this.uri = uri;
        this.defaultAccessPolicyDefinitionId = defaultAccessPolicyDefinitionId;
        this.defaultContractPolicyDefinitionId = defaultContractPolicyDefinitionId;
    }


    /**
     * Returns whether to register a referred element. Does not
     *
     * @param reference Element to register or not.
     * @return Whether to register it.
     */
    public abstract boolean eligibleForRegistration(Reference reference);


    /**
     * Get the full URI to access this AAS repository, including
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
    public abstract static class AbstractBuilder<T extends AasServerContext, B extends AasServerContext.AbstractBuilder<T, B>> {
        /** Default access policy definition ID to set. */
        protected String defaultAccessPolicyDefinitionId;
        /** Default contract policy definition ID to set. */
        protected String defaultContractPolicyDefinitionId;
        /** URI of the AAS server. */
        protected URI uri;


        /** Default constructor. */
        public AbstractBuilder() {
        }


        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }


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
         * Sets the default access policy definition ID.
         *
         * @param defaultAccessPolicyDefinitionId the default access policy definition ID.
         * @return this builder.
         */
        public B defaultAccessPolicyDefinitionId(String defaultAccessPolicyDefinitionId) {
            this.defaultAccessPolicyDefinitionId = defaultAccessPolicyDefinitionId;
            return self();
        }


        /**
         * Sets the default contract policy definition ID.
         *
         * @param defaultContractPolicyDefinitionId the default contract policy definition ID.
         * @return this builder.
         */
        public B defaultContractPolicyDefinitionId(String defaultContractPolicyDefinitionId) {
            this.defaultContractPolicyDefinitionId = defaultContractPolicyDefinitionId;
            return self();
        }


        /**
         * Validates and fills in default values for unset fields.
         */
        protected void validate() {
            defaultAccessPolicyDefinitionId = Objects.requireNonNullElse(defaultAccessPolicyDefinitionId, DEFAULT_ACCESS_POLICY_DEFINITION_ID);
            defaultContractPolicyDefinitionId = Objects.requireNonNullElse(defaultContractPolicyDefinitionId, DEFAULT_USAGE_POLICY_DEFINITION_ID);
        }
    }
}
