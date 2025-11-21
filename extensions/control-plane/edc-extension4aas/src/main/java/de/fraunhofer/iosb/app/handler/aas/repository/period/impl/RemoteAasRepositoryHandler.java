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
package de.fraunhofer.iosb.app.handler.aas.repository.period.impl;

import de.fraunhofer.iosb.app.handler.aas.RemoteAasHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.remote.impl.RemoteAasRepositoryClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;


/**
 * Handler for all AAS repositories that are registered via their URI / HTTP endpoints. The difference to the local handlers is that here, we have to poll the AAS repository
 * periodically instead of listening to events.
 */
public class RemoteAasRepositoryHandler extends RemoteAasHandler<RemoteAasRepositoryClient> {

    /**
     * Create a new remote AAS repository handler and populate EDC stores.
     *
     * @param monitor Log messages.
     * @param client Client to communicate with remote AAS repository.
     * @param edcStoreHandler Keep EDC stores up-to-date
     * @throws UnauthorizedException Initial connection to the repository failed due to unauthorized error.
     * @throws ConnectException Initial connection to the repository failed due to connection error.
     */
    public RemoteAasRepositoryHandler(Monitor monitor, RemoteAasRepositoryClient client, EdcStoreHandler edcStoreHandler) throws UnauthorizedException,
            ConnectException {
        super(monitor, client, edcStoreHandler);
    }


    @Override
    protected Environment getEnvironment() throws UnauthorizedException, ConnectException {
        return client.getEnvironment();
    }


    @Override
    protected SubmodelElement mapSubmodelElement(Reference reference, SubmodelElement submodelElement) {
        return null;
    }


    @Override
    protected SubmodelElement filterSubmodelElementStructure(Reference reference, SubmodelElement submodelElement) {
        return null;
    }

}
