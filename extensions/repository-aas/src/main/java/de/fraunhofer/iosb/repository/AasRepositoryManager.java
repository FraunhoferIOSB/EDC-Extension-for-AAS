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
package de.fraunhofer.iosb.repository;

import de.fraunhofer.iosb.model.config.AasRepositoryConfig;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;

import java.net.URI;


/**
 * Manages a specific type of AAS repositories like FA³ST or BaSyx
 */
public interface AasRepositoryManager<CFG extends AasRepositoryConfig<?>> {

    /**
     * Boot up an AAS repository by environment, an open port will be used.
     *
     * @param config AAS repository configuration
     * @return A repository context containing meta-information and possibly accessor logic to the AAS repository.
     */
    <CTX extends AasRepositoryContext> CTX startRepository(CFG config);


    /**
     * Stop all running AAS repositories that were started by this manager
     */
    void stopAll();


    /**
     * Stop an AAS repository by its URI
     *
     * @param repositoryUri URI of AAS repository to be stopped
     */
    void stopRepository(URI repositoryUri);
}
