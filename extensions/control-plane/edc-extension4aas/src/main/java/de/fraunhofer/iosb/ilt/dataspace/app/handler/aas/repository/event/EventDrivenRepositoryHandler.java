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
package de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.event;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.aas.repository.AasRepositoryHandler;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.local.impl.LocalFaaastRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;

import java.util.Map;
import java.util.function.BiFunction;


/**
 * Handler for any event-driven AAS repository, i.e. repositories where the corresponding clients allow subscriptions to
 * events for created/updated/deleted AAS elements.
 */
public abstract class EventDrivenRepositoryHandler extends AasRepositoryHandler<LocalFaaastRepositoryClient, LocalFaaastRepositoryContext> {

    /**
     * Creates a new event-driven repository handler.
     *
     * @param monitor Monitor used for log outputs.
     * @param context Context holding information specifically about a FA³ST service.
     * @param vault Provides secrets such as certificates and keys.
     * @param edcStoreHandler Handler to manage registration of EDC assets, policies and contracts.
     */
    protected EventDrivenRepositoryHandler(Monitor monitor, LocalFaaastRepositoryContext context, Vault vault, EdcStoreHandler edcStoreHandler) {
        super(monitor, context, vault, edcStoreHandler);
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


    /**
     * Maps the referable referenced by the given reference (resolved against the given environment) to an EDC asset.
     * Identifiables are mapped using the identifiable mapper,
     * submodel elements using the submodel element mapper.
     *
     * @param reference Reference of the AAS element to map.
     * @param environment The environment used to resolve the reference.
     * @return The mapped EDC asset.
     */
    private Asset referenceToAsset(Reference reference, Environment environment) {
        Referable referable = AasUtils.resolve(reference, environment);

        Asset mapped;
        if (referable instanceof Identifiable identifiable) {
            mapped = identifiableMapper.map(identifiable);
        }
        else if (referable instanceof SubmodelElement submodelElement) {
            mapped = submodelElementMapper.map(ReferenceHelper.getParent(reference), submodelElement);
        }
        else {
            throw new EdcException("Could not resolve event message reference.");
        }

        return mapped;
    }
}
