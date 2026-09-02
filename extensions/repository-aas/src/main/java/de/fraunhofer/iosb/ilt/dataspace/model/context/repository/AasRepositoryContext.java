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
package de.fraunhofer.iosb.ilt.dataspace.model.context.repository;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.util.InetTools;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * Context holding information about an AAS repository.
 */
public abstract class AasRepositoryContext extends AasServerContext {

    private final List<PolicyBinding> policyBindings;
    private final boolean onlySubmodels;


    /**
     * Creates a new AAS repository context.
     *
     * @param uri the URI of the AAS repository.
     * @param defaultAccessPolicyDefinitionId the default access policy definition ID.
     * @param defaultContractPolicyDefinitionId the default contract policy definition ID.
     * @param policyBindings policy bindings for this repository.
     * @param onlySubmodels whether only submodels should be registered.
     */
    protected AasRepositoryContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId, List<PolicyBinding> policyBindings,
                                   boolean onlySubmodels) {
        super(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId);
        this.policyBindings = policyBindings;
        this.onlySubmodels = onlySubmodels;
    }


    /**
     * Availability check for this AAS repository.
     *
     * @return true if the repository is available. By default, this checks if the URI of the repository is reachable.
     */
    public boolean isAvailable() {
        return InetTools.pingHost(getUri().getHost(), getUri().getPort());
    }


    /**
     * Returns a policy binding for a given reference. Warning: this function returns a policy binding regardless of whether
     * an element should be registered or not. Use
     * {@link #eligibleForRegistration(Reference reference)} for that.
     *
     * @param reference Reference for which a policy binding is to be returned.
     * @return The policy binding.
     */
    public PolicyBinding getPolicyBinding(Reference reference) {
        return getPolicyBindings(reference).stream()
                .findFirst()
                .orElse(new PolicyBinding(reference, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId));
    }


    /**
     * Returns all policy bindings for a given reference. When no explicit policy bindings are configured (register-all
     * mode), a single default binding is returned. Otherwise, only bindings whose referred element matches the given
     * reference are returned (possibly empty).
     *
     * @param reference Reference for which policy bindings are to be returned.
     * @return List of policy bindings for the reference (never null, possibly empty in selective mode).
     */
    public List<PolicyBinding> getPolicyBindings(Reference reference) {
        if (policyBindings.isEmpty()) {
            return List.of(new PolicyBinding(reference, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId));
        }
        return policyBindings.stream()
                .filter(policyBinding -> Objects.equals(reference, policyBinding.referredElement()))
                .toList();
    }


    @Override
    public boolean eligibleForRegistration(Reference reference) {
        return (policyBindings.isEmpty() || policyBindingIfPresent(reference).isPresent()) &&
                (ReferenceHelper.getEffectiveKeyType(reference) == KeyTypes.SUBMODEL || !isOnlySubmodels());
    }


    /**
     * Returns references that shall be registered by this extension. If all elements shall be registered, the optional will
     * be empty.
     *
     * @return References to register to EDC.
     */
    public List<Reference> getReferences() {
        return policyBindings.stream()
                .map(PolicyBinding::referredElement).toList();
    }


    /**
     * Returns whether only submodels are to be registered.
     *
     * @return True if only submodels are to be registered, else false.
     */
    public boolean isOnlySubmodels() {
        return onlySubmodels;
    }


    private Optional<PolicyBinding> policyBindingIfPresent(Reference reference) {
        return policyBindings.stream()
                .filter(policyBinding -> Objects.equals(reference, policyBinding.referredElement()))
                .findFirst();
    }


    /**
     * Abstract builder for AAS repository contexts.
     *
     * @param <T> the context type produced by this builder.
     * @param <B> the builder subtype for fluent chaining.
     */
    public abstract static class AbstractBuilder<T extends AasRepositoryContext, B extends AasServerContext.AbstractBuilder<T, B>> extends AasServerContext.AbstractBuilder<T, B> {
        /** URI of the AAS repository. */
        protected URI uri;
        /** Policy bindings to set on the context. */
        protected List<PolicyBinding> policyBindings;
        /** Whether only submodels should be registered. */
        protected boolean onlySubmodels;


        /** Default constructor. */
        protected AbstractBuilder() {}


        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
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
         * Sets the URI of the AAS repository.
         *
         * @param uri the repository URI.
         * @return this builder.
         */
        public B uri(URI uri) {
            this.uri = uri;
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
         * Validates and fills in default values for unset fields.
         */
        protected void validate() {
            super.validate();
            Objects.requireNonNull(uri, "Access URI must be non-null");
            policyBindings = Objects.requireNonNullElse(policyBindings, new ArrayList<>());
        }
    }

}
