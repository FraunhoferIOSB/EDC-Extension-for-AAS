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
package de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository;

import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.AasHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;


/**
 * Abstract handler for AAS repositories, providing access to the repository environment and the policy bindings
 * configured per reference.
 *
 * @param <C> AAS repository client implementation to communicate with the AAS repository.
 * @param <CTX> Context holding information about an AAS server.
 */
public abstract class AasRepositoryHandler<C extends AasRepositoryClient, CTX extends AasServerContext> extends AasHandler<C, CTX> {

    /**
     * Creates a new AAS repository handler.
     *
     * @param monitor Monitor used for log outputs.
     * @param context Context holding information about an AAS repository.
     * @param vault Provides secrets such as certificates and keys.
     * @param edcStoreHandler Handler to manage registration of EDC assets, policies and contracts.
     */
    protected AasRepositoryHandler(Monitor monitor, CTX context, Vault vault, EdcStoreHandler edcStoreHandler) {
        super(monitor, context, vault, edcStoreHandler);
    }


    @Override
    protected Environment getEnvironment() throws ConnectivityException, StatusCodeException {
        return client.getEnvironment();
    }
}
