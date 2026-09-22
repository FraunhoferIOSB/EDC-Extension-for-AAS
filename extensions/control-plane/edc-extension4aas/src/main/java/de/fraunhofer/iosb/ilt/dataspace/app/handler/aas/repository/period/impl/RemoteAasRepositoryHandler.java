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
package de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.period.impl;

import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.RemoteAasHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.remote.impl.RemoteAasRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote.RemoteAasRepositoryContext;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;


/**
 * Handler for all AAS repositories that are registered via their URI / HTTP endpoints. The difference to the local
 * handlers is that here, we have to poll the AAS repository
 * periodically instead of listening to events.
 */
public class RemoteAasRepositoryHandler extends RemoteAasHandler<RemoteAasRepositoryClient, RemoteAasRepositoryContext> {

    /**
     * Create a new remote AAS repository handler and populate EDC stores.
     *
     * @param monitor Log messages.
     * @param vault Provides secrets such as certificates and keys.
     * @param context Context holding information about an AAS repository.
     * @param edcStoreHandler Keep EDC stores up-to-date
     * @throws StatusCodeException Initial connection to the repository failed due to unauthorized error.
     * @throws ConnectivityException Initial connection to the repository failed due to connection error.
     */
    public RemoteAasRepositoryHandler(Monitor monitor, Vault vault, RemoteAasRepositoryContext context, EdcStoreHandler edcStoreHandler) throws StatusCodeException,
            ConnectivityException {
        super(monitor, context, vault, edcStoreHandler);
    }


    @Override
    protected RemoteAasRepositoryClient clientFrom(Vault vault, RemoteAasRepositoryContext context) {
        return new RemoteAasRepositoryClient(vault, context);
    }


    @Override
    protected Environment getEnvironment() throws ConnectivityException, StatusCodeException {
        return client.getEnvironment();
    }

}
