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
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.client.repository.local.LocalAasRepositoryClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;

import java.net.ConnectException;
import java.util.Map;
import java.util.function.BiFunction;


public abstract class EventDrivenRepositoryHandler<C extends LocalAasRepositoryClient<?>> extends AasRepositoryHandler<C> {

    protected EventDrivenRepositoryHandler(Monitor monitor, C client, EdcStoreHandler edcStoreHandler) {
        super(monitor, client, edcStoreHandler);
    }


    @Override
    public Map<PolicyBinding, Asset> initialize() throws UnauthorizedException, ConnectException {
        var map = super.initialize();
        subscribe();
        return map;
    }


    @Override
    public void cleanUp() {
        super.cleanUp();
        unsubscribe();
    }


    protected abstract void subscribe();


    protected abstract void unsubscribe();


    protected StoreResult<Void> doHandle(Reference reference, BiFunction<PolicyBinding, Asset, StoreResult<Void>> consumer) {
        PolicyBinding policyBinding = policyBindingFor(reference);
        Asset asset = referenceToAsset(reference, client.getEnvironment());

        StoreResult<Void> result = consumer.apply(policyBinding, asset);

        if (result.failed()) {
            result.getFailureMessages().add(0, String.format("Asset with id %s from repository %s could not be processed", asset.getId(),
                    client.getUri()));
        }

        return result;
    }
}
