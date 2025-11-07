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
package de.fraunhofer.iosb.app.handler.aas.repository.event.impl;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.handler.aas.repository.event.EventDrivenRepositoryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.client.repository.local.impl.LocalFaaastRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.EventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionId;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.event.change.ElementChangeEventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.event.change.ElementCreateEventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.event.change.ElementDeleteEventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.event.change.ElementUpdateEventMessage;
import de.fraunhofer.iosb.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class LocalFaaastRepositoryHandler extends EventDrivenRepositoryHandler {

    private final List<SubscriptionId> subscriptions = new ArrayList<>();

    public LocalFaaastRepositoryHandler(Monitor monitor, LocalFaaastRepositoryContext faaastContext, EdcStoreHandler edcStoreHandler) {
        super(monitor, faaastContext, edcStoreHandler,
                new LocalFaaastRepositoryClient(faaastContext));
        initialize();
    }

    @Override
    protected void subscribe() {
        if (client instanceof LocalFaaastRepositoryClient faaastClient) {
            subscriptions.add(faaastClient.subscribeTo(ElementCreateEventMessage.class, this::created));
            subscriptions.add(faaastClient.subscribeTo(ElementDeleteEventMessage.class, this::deleted));
            subscriptions.add(faaastClient.subscribeTo(ElementUpdateEventMessage.class, this::updated));
        } else {
            throw new IllegalStateException("FA³ST repository handler does not own a FA³ST client.");
        }
    }

    @Override
    protected void unsubscribe() {
        if (client instanceof LocalFaaastRepositoryClient faaastClient) {
            subscriptions.forEach(faaastClient::unsubscribeFrom);
        } else {
            throw new IllegalStateException("FA³ST repository handler does not own a FA³ST client.");
        }
    }

    private void updated(ElementUpdateEventMessage message) {
        doHandleWrap(message, this::updateSingle);
    }

    private void created(ElementCreateEventMessage message) {
        doHandleWrap(message, this::registerSingle);
    }

    private void deleted(ElementChangeEventMessage message) {
        doHandleWrap(message, this::unregisterSingle);
    }

    /* Don't throw exceptions to FA³ST to prevent message bus from crashing. */
    private void doHandleWrap(ElementChangeEventMessage message, BiFunction<PolicyBinding, Asset, StoreResult<Void>> consumer) {
        try {
            doHandle(message, consumer);
        } catch (Exception e) {
            monitor.severe("Exception thrown while handling event", e);
        }
    }

    private void doHandle(ElementChangeEventMessage message, BiFunction<PolicyBinding, Asset, StoreResult<Void>> consumer) {
        if (messageInvalid(message)) {
            return;
        }

        StoreResult<Void> result = super.doHandle(message.getElement(), consumer);

        String eventMessageName = message.getClass().getSimpleName();
        if (result.failed()) {
            monitor.warning(String.format("Failed handling %s: %s", eventMessageName, result.getFailureDetail()));
        } else {
            monitor.info(String.format("Handled %s from repository %s.", eventMessageName, client.getUri()));
        }
    }

    private boolean messageInvalid(EventMessage message) {
        Reference reference = message.getElement();

        return reference == null || !referenceFilter().test(reference);
    }
}
