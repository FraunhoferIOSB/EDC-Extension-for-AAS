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


public abstract class AasRepositoryConfig<C> {

    protected final List<PolicyBinding> policyBindings;


    protected AasRepositoryConfig(List<PolicyBinding> policyBindings) {
        this.policyBindings = policyBindings;
    }


    public abstract C get();


    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }


    protected abstract static class Builder<B extends Builder<B, C>, C extends AasRepositoryConfig<?>> {
        protected Path model;
        protected List<PolicyBinding> policyBindings;


        public abstract B self();


        public B model(Path pathToModel) {
            this.model = pathToModel;
            return self();
        }


        public B policyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
            return self();
        }


        public B model(String pathToModel) {
            this.model = Optional.ofNullable(pathToModel).map(Path::of).orElse(null);

            return self();
        }


        public abstract C build();
    }
}
