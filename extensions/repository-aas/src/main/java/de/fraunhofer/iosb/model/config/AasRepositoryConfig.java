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
package de.fraunhofer.iosb.model.config;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


/** Abstract base configuration for an AAS repository.
 *
 * @param <C> the concrete configuration type.
 */
public abstract class AasRepositoryConfig<C> {

    /** Policy bindings that map AAS elements to access and usage policies. */
    protected final List<PolicyBinding> policyBindings;
    /** Whether only submodels should be registered. */
    protected final boolean onlySubmodels;


    /**
     * Creates a new repository configuration.
     *
     * @param policyBindings policy bindings for this repository.
     * @param onlySubmodels whether only submodels should be registered.
     */
    protected AasRepositoryConfig(List<PolicyBinding> policyBindings, boolean onlySubmodels) {
        this.policyBindings = policyBindings;
        this.onlySubmodels = onlySubmodels;
    }


    /**
     * Returns the concrete configuration instance.
     *
     * @return the concrete configuration.
     */
    public abstract C get();


    /**
     * Returns the policy bindings of this repository.
     *
     * @return the policy bindings.
     */
    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }


    /**
     * Returns whether only submodels should be registered.
     *
     * @return true if only submodels should be registered.
     */
    public boolean isOnlySubmodels() {
        return onlySubmodels;
    }


    /**
     * Abstract builder for AAS repository configurations.
     *
     * @param <B> the builder subtype.
     * @param <C> the configuration type produced by this builder.
     */
    protected abstract static class Builder<B extends Builder<B, C>, C extends AasRepositoryConfig<?>> {
        /** Path to the AAS model file. */
        protected Path model;
        /** Policy bindings to set on the configuration. */
        protected List<PolicyBinding> policyBindings;
        /** Whether only submodels should be registered. */
        protected boolean onlySubmodels;


        /** Default constructor. */
        public Builder() {
        }


        /**
         * Returns this builder instance for fluent chaining.
         *
         * @return this builder.
         */
        public abstract B self();


        /**
         * Sets the path to the AAS model file.
         *
         * @param pathToModel path to the model file.
         * @return this builder.
         */
        public B model(Path pathToModel) {
            this.model = pathToModel;
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
         * Sets the path to the AAS model file from a string.
         *
         * @param pathToModel path to the model file as string.
         * @return this builder.
         */
        public B model(String pathToModel) {
            this.model = Optional.ofNullable(pathToModel).map(Path::of).orElse(null);

            return self();
        }


        /**
         * Builds the configuration instance.
         *
         * @return the built configuration.
         */
        public abstract C build();
    }
}
