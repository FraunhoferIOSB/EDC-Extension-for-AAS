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
package de.fraunhofer.iosb.ilt.dataspace.app.handler.aas;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.RemoteHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.util.DiffHelper;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.util.MappingHelper;
import de.fraunhofer.iosb.ilt.dataspace.client.AasServerClient;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;

import java.util.Map;


/**
 * Superclass for remote AAS server handlers that implement Runnable to periodically fetch updates from AAS servers.
 *
 * @param <C> AAS server client implementation to communicate with the AAS server.
 */
public abstract class RemoteAasHandler<C extends AasServerClient> extends AasHandler<C> implements RemoteHandler {

    // This map keeps tabs on the current state of registered assets/contracts.
    // If an asset or its contract could not be registered, they will not appear in this map.
    // We keep this "cache" to not flood the Asset/ContractStores with requests.
    /** Currently registered assets keyed by their policy binding. */
    protected final Map<PolicyBinding, Asset> registeredAssets;


    /**
     * Creates a new remote AAS handler and initializes the registered assets from the AAS server.
     *
     * @param monitor Monitor used for log outputs.
     * @param client Client used to communicate with the AAS server.
     * @param edcStoreHandler Handler to manage registration of EDC assets, policies and contracts.
     * @throws StatusCodeException if a call to the AAS server returned a status code other than 2xx.
     * @throws ConnectivityException if a connection to the AAS server could not be established.
     */
    protected RemoteAasHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) throws StatusCodeException, ConnectivityException {
        super(monitor, client, edcStoreHandler);
        registeredAssets = initialize();
    }


    /**
     * Performs synchronization between the AAS environment and the EDC AssetIndex/ContractDefinitionStore.
     * <p>
     * Calling run() assumes that initialization succeeded, where a connection to the AAS server was successful. If, at a
     * later time, authorization changes or the connection to the
     * AAS server fails, it will be logged and treated as a transient error.
     */
    @Override
    public void run() {
        if (!client.isAvailable()) {
            monitor.warning(String.format("%s unavailable", client.getUri()));
            return;
        }
        Environment currentEnvironment;
        try {
            currentEnvironment = getEnvironment();
        }
        catch (StatusCodeException e) {
            monitor.warning(String.format("StatusCodeException when connecting to %s", client.getUri()), e);
            return;
        }
        catch (ConnectivityException e) {
            monitor.warning(String.format("Could not connect to %s", client.getUri()), e);
            return;
        }

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        Map<PolicyBinding, Asset> updatedAssets = expandBindings(mapped);

        // All elements that are not currently registered (as far as we know) shall be registered
        DiffHelper.getToAdd(registeredAssets, updatedAssets).entrySet().stream()
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> registeredAssets.put(entry.getKey(), entry.getValue()));

        // All elements that are currently registered (as far as we know) but should not be shall be unregistered
        DiffHelper.getToRemove(registeredAssets, updatedAssets).entrySet().stream()
                .filter(entry -> unregisterSingle(entry.getKey(), entry.getValue().getId()).succeeded())
                .forEach(entry -> registeredAssets.remove(entry.getKey(), entry.getValue()));

        // All elements to update (policy bindings are not modifiable, thus not need to be checked) shall be updated
        DiffHelper.getToUpdate(registeredAssets, updatedAssets).entrySet().stream()
                .filter(entry -> updateSingle(entry.getValue()).succeeded())
                .forEach(entry -> registeredAssets.put(entry.getKey(), entry.getValue()));
    }


    @Override
    protected Map<PolicyBinding, Asset> getCurrentlyRegistered() {
        return registeredAssets;
    }
}
