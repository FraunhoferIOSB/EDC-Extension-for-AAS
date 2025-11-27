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

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_POLICY_DEFINITION_ID;


public abstract class AasServerContext {

    protected final String defaultAccessPolicyDefinitionId;
    protected final String defaultContractPolicyDefinitionId;
    private final URI uri;


    protected AasServerContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId) {
        this.uri = uri;
        this.defaultAccessPolicyDefinitionId = defaultAccessPolicyDefinitionId;
        this.defaultContractPolicyDefinitionId = defaultContractPolicyDefinitionId;
    }


    /**
     * Returns whether to register a referred element.
     *
     * @param reference Element to register or not.
     * @return Whether to register it.
     */
    public abstract boolean doRegister(Reference reference);


    /**
     * Get the full URI to access this AAS repository, including
     *
     * @return The full accessor URI for this repository, e.g., <a href="">https://my-aas:1337/api/v3.0</a>.
     */
    public URI getUri() {
        return uri;
    }


    public abstract static class AbstractBuilder<T extends AasServerContext, B extends AasServerContext.AbstractBuilder<T, B>> {
        protected String defaultAccessPolicyDefinitionId;
        protected String defaultContractPolicyDefinitionId;
        protected URI uri;


        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }


        public B uri(URI uri) {
            this.uri = uri;
            return self();
        }


        public B defaultAccessPolicyDefinitionId(String defaultAccessPolicyDefinitionId) {
            this.defaultAccessPolicyDefinitionId = defaultAccessPolicyDefinitionId;
            return self();
        }


        public B defaultContractPolicyDefinitionId(String defaultContractPolicyDefinitionId) {
            this.defaultContractPolicyDefinitionId = defaultContractPolicyDefinitionId;
            return self();
        }


        protected void validate() {
            defaultAccessPolicyDefinitionId = Objects.requireNonNullElse(defaultAccessPolicyDefinitionId, DEFAULT_POLICY_DEFINITION_ID);
            defaultContractPolicyDefinitionId = Objects.requireNonNullElse(defaultContractPolicyDefinitionId, DEFAULT_POLICY_DEFINITION_ID);
        }
    }
}
