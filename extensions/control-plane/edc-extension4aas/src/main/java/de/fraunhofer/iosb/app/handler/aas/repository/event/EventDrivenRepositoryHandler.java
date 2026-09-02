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
package de.fraunhofer.iosb.app.handler.aas.repository.event;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.repository.local.LocalAasRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Map;
import java.util.function.BiFunction;


/**
 * Handler for any event-driven AAS repository, i.e. repositories where the corresponding clients allow subscriptions to
 * events for created/updated/deleted AAS elements.
 *
 * @param <C> A client supporting subscribing to AAS repository events.
 */
public abstract class EventDrivenRepositoryHandler<C extends LocalAasRepositoryClient<?>> extends AasRepositoryHandler<C> {

    /**
     * Creates a new event-driven repository handler.
     *
     * @param monitor Monitor used for log outputs.
     * @param client Client used to communicate with the AAS repository and to subscribe to events.
     * @param edcStoreHandler Handler to manage registration of EDC assets, policies and contracts.
     */
    protected EventDrivenRepositoryHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        super(monitor, client, edcStoreHandler);
    }


    @Override
    public Map<PolicyBinding, Asset> initialize() throws StatusCodeException, ConnectivityException {
        var map = super.initialize();
        subscribe();
        return map;
    }


    @Override
    public void cleanUp() {
        super.cleanUp();
        unsubscribe();
    }


    /**
     * Subscribes the handler to the relevant AAS repository events (create/update/delete).
     */
    protected abstract void subscribe();


    /**
     * Unsubscribes the handler from the AAS repository events.
     */
    protected abstract void unsubscribe();


    /**
     * Handles the element referenced by the given reference by applying the consumer to each of its policy-binding-
     * specific assets.
     *
     * @param reference Reference of the AAS element to handle.
     * @param consumer Function applied to each (policy binding, asset) pair, returning a store result.
     * @return The aggregated store result of the operation.
     */
    protected StoreResult<Void> doHandle(Reference reference, BiFunction<PolicyBinding, Asset, StoreResult<Void>> consumer) {
        Asset baseAsset = referenceToAsset(reference, client.getEnvironment());

        StoreResult<Void> result = StoreResult.success();
        for (PolicyBinding binding: policyBindingsFor(reference)) {
            Asset asset = assetForBinding(reference, baseAsset, binding);
            StoreResult<Void> singleResult = consumer.apply(binding, asset);

            if (singleResult.failed()) {
                singleResult.getFailureMessages().add(0, String.format("Asset with id %s from repository %s could not be processed", asset.getId(),
                        client.getUri()));
                return singleResult;
            }
            result = singleResult;
        }
        return result;
    }
}
