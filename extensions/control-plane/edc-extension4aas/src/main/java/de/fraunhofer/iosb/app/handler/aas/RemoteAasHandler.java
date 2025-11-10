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
package de.fraunhofer.iosb.app.handler.aas;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.handler.RemoteHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.handler.util.DiffHelper;
import de.fraunhofer.iosb.app.handler.util.MappingHelper;
import de.fraunhofer.iosb.client.AasServerClient;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;

import java.net.ConnectException;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Superclass for remote AAS server handlers that implement Runnable to periodically fetch updates from AAS servers.
 */
public abstract class RemoteAasHandler<C extends AasServerClient> extends AasHandler<C> implements RemoteHandler {

    // This map keeps tabs on the current state of registered assets/contracts.
    // If an asset or its contract could not be registered, they will not appear in this map.
    // We keep this "cache" to not flood the Asset/ContractStores with requests.
    protected final Map<PolicyBinding, Asset> referenceAssetMapping;

    protected RemoteAasHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) throws UnauthorizedException,
            ConnectException {
        super(monitor, client, edcStoreHandler);
        referenceAssetMapping = initialize();
    }

    @Override
    protected Map<PolicyBinding, Asset> getCurrentlyRegistered() {
        return referenceAssetMapping;
    }

    @Override
    public void run() {
        if (!client.isAvailable()) {
            monitor.warning(String.format("%s unavailable", client.getUri()));
            return;
        }
        Environment currentEnvironment;
        try {
            currentEnvironment = getEnvironment();
        } catch (UnauthorizedException e) {
            monitor.warning(String.format("Unauthorized exception when connecting to %s", client.getUri()), e);
            return;
        } catch (ConnectException e) {
            monitor.warning(String.format("Could not connect to %s", client.getUri()), e);
            return;
        }

        Map<Reference, Asset> mapped = MappingHelper.map(currentEnvironment, identifiableMapper::map, submodelElementMapper::map);

        Map<PolicyBinding, Asset> filtered = mapped.entrySet().stream()
                .filter(entry -> referenceFilter().test(entry.getKey()))
                .map(entry -> Map.entry(policyBindingFor(entry.getKey()), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // All elements that are not currently registered (as far as we know)
        Map<PolicyBinding, Asset> toAdd = DiffHelper.getToAdd(referenceAssetMapping, filtered);
        // All elements that are currently registered (as far as we know) but should not
        Map<PolicyBinding, Asset> toRemove = DiffHelper.getToRemove(referenceAssetMapping, filtered);
        // All elements to update (policy bindings are not modifiable, thus not need to be checked)
        Map<PolicyBinding, Asset> toUpdate = DiffHelper.getToUpdate(referenceAssetMapping, filtered);

        toAdd.entrySet().stream()
                .filter(entry -> registerSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.put(entry.getKey(), entry.getValue()));

        toRemove.entrySet().stream()
                .filter(entry -> unregisterSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.remove(entry.getKey(), entry.getValue()));

        toUpdate.entrySet().stream()
                .filter(entry -> updateSingle(entry.getKey(), entry.getValue()).succeeded())
                .forEach(entry -> referenceAssetMapping.put(entry.getKey(), entry.getValue()));
    }
}
