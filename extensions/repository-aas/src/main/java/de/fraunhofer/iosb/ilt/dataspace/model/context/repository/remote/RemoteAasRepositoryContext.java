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
package de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import de.fraunhofer.iosb.ilt.dataspace.model.context.RemoteClientContext;

import java.net.URI;
import java.util.List;


/**
 * Context holding information about an AAS repository.
 */
public class RemoteAasRepositoryContext extends AasServerContext implements RemoteClientContext {

    /** Error message template used when remote repository operations fail. */
    public static final String ERR_MSG_TEMPLATE = "%s from %s failed.";


    private RemoteAasRepositoryContext(URI uri, List<PolicyBinding> policyBindings,
                                       AuthenticationMethod authenticationMethod, boolean allowSelfSigned, boolean onlySubmodels) {
        super(uri, policyBindings, onlySubmodels, authenticationMethod, allowSelfSigned);
    }


    /**
     * Builder for {@link RemoteAasRepositoryContext}.
     */
    public static class Builder extends AbstractBuilder<RemoteAasRepositoryContext, Builder> {


        /** Default constructor. */
        public Builder() {}


        @Override
        protected Builder self() {
            return this;
        }


        /**
         * Builds the {@link RemoteAasRepositoryContext} instance.
         *
         * @return the built context.
         */
        public RemoteAasRepositoryContext build() {
            super.validate();
            return new RemoteAasRepositoryContext(uri, policyBindings, authenticationMethod, allowSelfSigned,
                    onlySubmodels);
        }
    }
}
