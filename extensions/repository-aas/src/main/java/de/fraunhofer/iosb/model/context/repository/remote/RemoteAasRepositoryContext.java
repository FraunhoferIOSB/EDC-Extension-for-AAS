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
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.List;

public class RemoteAasRepositoryContext extends AasRepositoryContext {
    public static final String ERR_MSG_TEMPLATE = "%s from %s failed.";
    @NotNull
    private final AuthenticationMethod authenticationMethod;

    private RemoteAasRepositoryContext(URI uri, List<PolicyBinding> policyBindings, AuthenticationMethod authenticationMethod) {
        super(uri, policyBindings);
        this.authenticationMethod = authenticationMethod;
    }
//
//    /**
//     * For a given reference, get the referable from this FA³ST repository. Can be an identifiable as well.
//     *
//     * @param reference The reference for the referable to get.
//     * @param clazz     The actual class of the reference.
//     * @param <R>       Type of the referable.
//     * @return The referable referenced by the reference.
//     */
//    public <R extends Referable> R getReferable(Reference reference, Class<R> clazz) throws NotFoundException, UnauthorizedException,
//            ConnectException {
//        if (EXTERNAL_REFERENCE == ReferenceHelper.determineReferenceType(reference)) {
//            throw new IllegalArgumentException("Cannot get referable by external reference.");
//        }
//
//        if (reference.getKeys().isEmpty()) {
//            throw new NotFoundException("Reference has no keys.");
//        }
//
//        return client.getReferable(reference, clazz);
//    }

    public @NotNull AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public static class Builder extends AbstractBuilder<RemoteAasRepositoryContext, Builder> {
        private AuthenticationMethod authenticationMethod;

        public Builder() {
        }

        public Builder authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }

        public RemoteAasRepositoryContext build() {
            super.validate();

            return new RemoteAasRepositoryContext(uri, policyBindings, authenticationMethod);
        }
    }
}
