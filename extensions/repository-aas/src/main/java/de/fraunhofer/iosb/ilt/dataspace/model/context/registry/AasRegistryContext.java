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
package de.fraunhofer.iosb.ilt.dataspace.model.context.registry;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import de.fraunhofer.iosb.ilt.dataspace.model.context.RemoteClientContext;

import java.net.URI;
import java.util.List;
import java.util.Objects;


/** Context holding information about an AAS registry. */
public class AasRegistryContext extends AasServerContext implements RemoteClientContext {

    private AasRegistryContext(URI uri, List<PolicyBinding> policyBindings,
                               boolean onlySubmodels, AuthenticationMethod authenticationMethod, boolean allowSelfSigned) {
        super(uri, policyBindings, onlySubmodels, authenticationMethod, allowSelfSigned);
    }


    /**
     * Builder for {@link AasRegistryContext}.
     */
    public static class Builder extends AbstractBuilder<AasRegistryContext, Builder> {

        /** Default constructor. */
        public Builder() {}


        @Override
        protected Builder self() {
            return this;
        }


        /**
         * Builds the {@link AasRegistryContext} instance.
         *
         * @return the built context.
         */
        public AasRegistryContext build() {
            Objects.requireNonNull(uri, "Access URI must be non-null");
            return new AasRegistryContext(uri, policyBindings, onlySubmodels, authenticationMethod,
                    allowSelfSigned);
        }
    }
}
