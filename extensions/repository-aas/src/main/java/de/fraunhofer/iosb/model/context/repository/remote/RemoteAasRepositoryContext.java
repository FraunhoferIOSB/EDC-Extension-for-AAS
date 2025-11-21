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
package de.fraunhofer.iosb.model.context.repository.remote;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;
import java.util.Objects;


public class RemoteAasRepositoryContext extends AasRepositoryContext {

    public static final String ERR_MSG_TEMPLATE = "%s from %s failed.";

    private final AuthenticationMethod authenticationMethod;
    private final boolean allowSelfSigned;


    private RemoteAasRepositoryContext(URI uri, String defaultAccessPolicyDefinitionId, String defaultContractPolicyDefinitionId, List<PolicyBinding> policyBindings,
                                       AuthenticationMethod authenticationMethod, boolean allowSelfSigned, boolean onlySubmodels) {
        super(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId, policyBindings, onlySubmodels);
        this.authenticationMethod = authenticationMethod;
        this.allowSelfSigned = allowSelfSigned;
    }


    public boolean allowSelfSigned() {
        return allowSelfSigned;
    }


    public @NotNull AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    public static class Builder extends AbstractBuilder<RemoteAasRepositoryContext, Builder> {
        private AuthenticationMethod authenticationMethod;
        private boolean allowSelfSigned;


        public Builder() {
        }


        public Builder authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }


        public Builder allowSelfSigned(boolean allowSelfSigned) {
            this.allowSelfSigned = allowSelfSigned;
            return this;
        }


        public RemoteAasRepositoryContext build() {
            super.validate();
            this.authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());

            return new RemoteAasRepositoryContext(uri, defaultAccessPolicyDefinitionId, defaultContractPolicyDefinitionId, policyBindings, authenticationMethod, allowSelfSigned,
                    onlySubmodels);
        }
    }
}
