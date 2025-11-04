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

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.lib.util.InetTools;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public abstract class AasRepositoryContext {

    private final URI uri;
    private List<PolicyBinding> policyBindings;


    protected AasRepositoryContext(URI uri) {
        this.uri = uri;
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
     * Get the full URI to access this AAS repository, including
     *
     * @return The full accessor URI for this repository, e.g., <a href="">https://my-aas:1337/api/v3.0</a>.
     */
    public URI getUri() {
        return uri;
    }

    public List<PolicyBinding> getPolicyBindings() {
        return policyBindings;
    }

    public void setPolicyBindings(List<PolicyBinding> policyBindings) {
        this.policyBindings = policyBindings;
    }

    public List<Reference> getReferences() {
        return policyBindings.stream().map(PolicyBinding::referredElement).toList();
    }

    public abstract static class AbstractBuilder<T extends AasRepositoryContext, B extends AbstractBuilder<T, B>> {
        protected URI uri;

        protected AbstractBuilder() {
        }

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }

        public B uri(URI uri) {
            this.uri = uri;
            return self();
        }

        protected void validate() {
            Objects.requireNonNull(uri, "Access URI must be non-null!");
        }
    }

}
