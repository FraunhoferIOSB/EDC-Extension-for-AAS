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

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.handler.util.DiffHelper;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.remote.impl.RemoteAasRepositoryClient;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.net.URI;
import java.util.Map;

public class RemoteAasRepositoryHandler extends AasRepositoryHandler implements RemoteHandler {

    private final RemoteAasRepositoryClient client;

    // This map keeps tabs on the current state of registered assets/contracts.
    // If an asset or its contract could not be registered, they will not appear in this map.
    // We keep this "cache" to not flood the Asset/ContractStores with requests.
    private final Map<PolicyBinding, Asset> referenceAssetMapping;


    public RemoteAasRepositoryHandler(Monitor monitor, RemoteAasRepositoryContext context, EdcStoreHandler edcStoreHandler) {
        super(monitor, context, edcStoreHandler);
        this.client = new RemoteAasRepositoryClient(context);
        referenceAssetMapping = initialize();
    }

    @Override
    public void run() {
        boolean repositoryAvailable = isAvailable();
        if (!repositoryAvailable) {
            monitor.warning(String.format("%s unavailable", client.getUri()));
            return;
        }

        Map<PolicyBinding, Asset> mappedEnvironment;
        try {
            mappedEnvironment = filterMap();
        } catch (ConnectException | UnauthorizedException clientException) {
            monitor.warning(String.format("Could not get environment from %s", client.getUri()), clientException);
            return;
        }

        // All elements that are not currently registered (as far as we know)
        Map<PolicyBinding, Asset> toAdd = DiffHelper.getToAdd(referenceAssetMapping, mappedEnvironment);
        // All elements that are currently registered (as far as we know) but should not
        Map<PolicyBinding, Asset> toRemove = DiffHelper.getToRemove(referenceAssetMapping, mappedEnvironment);
        // All elements to update (policy bindings are not modifiable, thus not need to be checked)
        Map<PolicyBinding, Asset> toUpdate = DiffHelper.getToUpdate(referenceAssetMapping, mappedEnvironment);

        toAdd.entrySet().stream()
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.put(entry.getKey(), entry.getValue()));

        toRemove.entrySet().stream()
                .filter(entry -> unregisterSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.remove(entry.getKey(), entry.getValue()));

        toUpdate.entrySet().stream()
                .filter(entry -> updateSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.remove(entry.getKey(), entry.getValue()));
    }


    @Override
    protected Environment getEnvironment() throws UnauthorizedException, ConnectException {
        return client.getEnvironment();
    }

    @Override
    protected boolean isAvailable() {
        return client.isAvailable();
    }

    @Override
    protected URI getUri() {
        return client.getUri();
    }

}
