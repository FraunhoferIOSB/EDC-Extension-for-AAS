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
package de.fraunhofer.iosb.model.context.repository;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.util.InetTools;
import de.fraunhofer.iosb.model.context.AasServerContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public abstract class AasRepositoryContext extends AasServerContext {

    private final List<PolicyBinding> policyBindings;
    private final boolean onlySubmodels;


    protected AasRepositoryContext(URI uri, List<PolicyBinding> policyBindings, boolean onlySubmodels) {
        super(uri);
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


    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }


    /**
     * Returns references that shall be registered by this extension. If all elements shall be registered, the optional will be empty.
     *
     * @return References to register to EDC.
     */
    public List<Reference> getReferences() {
        return policyBindings.stream()
                .map(PolicyBinding::referredElement).toList();
    }


    public boolean isOnlySubmodels() {
        return onlySubmodels;
    }


    public abstract static class AbstractBuilder<T extends AasRepositoryContext, B extends AbstractBuilder<T, B>> {
        protected URI uri;
        protected List<PolicyBinding> policyBindings;
        protected boolean onlySubmodels;


        protected AbstractBuilder() {
        }


        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }


        public B policyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
            return self();
        }


        public B uri(URI uri) {
            this.uri = uri;
            return self();
        }


        public B onlySubmodels(boolean onlySubmodels) {
            this.onlySubmodels = onlySubmodels;
            return self();
        }


        protected void validate() {
            Objects.requireNonNull(uri, "Access URI must be non-null");
            policyBindings = Objects.requireNonNullElse(policyBindings, new ArrayList<>());
        }
    }

}
