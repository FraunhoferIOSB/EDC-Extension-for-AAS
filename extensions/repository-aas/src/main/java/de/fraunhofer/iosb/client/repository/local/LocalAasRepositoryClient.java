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
package de.fraunhofer.iosb.client.repository.local;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;


/**
 * Any local AAS repository client can extend this class. See {@link de.fraunhofer.iosb.client.repository.local.impl.LocalFaaastRepositoryClient}
 *
 * @param <CTX> Context type of the AAS repository context.
 */
public abstract class LocalAasRepositoryClient<CTX extends AasRepositoryContext> implements AasRepositoryClient {
    protected final CTX context;


    protected LocalAasRepositoryClient(CTX context) {
        this.context = context;
    }


    @Override
    public URI getUri() {
        return context.getUri();
    }


    @Override
    public boolean isAvailable() {
        return context.isAvailable();
    }


    @Override
    public boolean eligibleForRegistration(Reference reference) {
        return context.eligibleForRegistration(reference);
    }


    @Override
    public PolicyBinding getPolicyBinding(Reference reference) {
        return context.getPolicyBinding(reference);
    }


    /**
     * Intentionally added this abstract function to signal no unauthorized/connect exceptions are thrown when using a subclass of LocalAasRepositoryClient.
     *
     * @return Environment.
     */
    public abstract Environment getEnvironment();
}
