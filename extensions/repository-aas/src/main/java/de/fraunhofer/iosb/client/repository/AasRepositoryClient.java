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
package de.fraunhofer.iosb.client.repository;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.ConnectException;


/**
 * Clients used to communicate with AAS repositories (e.g., FA³ST service).
 */
public interface AasRepositoryClient extends AasServerClient {

    /**
     * Return the environment that is stored with the AAS repository.
     *
     * @return Environment of AAS repository.
     * @throws ConnectException Connection to the repository failed.
     * @throws UnauthorizedException Not authorized to access resources of the repository.
     */
    Environment getEnvironment() throws ConnectException, UnauthorizedException;


    /**
     * Returns a policy binding stored with the context pertaining to the AAS repository behind this client.
     *
     * @param reference Reference of the policy binding to return
     * @return The policy binding.
     */
    PolicyBinding getPolicyBinding(Reference reference);
}
