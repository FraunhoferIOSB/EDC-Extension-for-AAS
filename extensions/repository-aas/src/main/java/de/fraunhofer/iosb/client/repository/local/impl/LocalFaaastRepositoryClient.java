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
package de.fraunhofer.iosb.client.repository.local.impl;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.client.repository.local.LocalAasRepositoryClient;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.EventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionId;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionInfo;
import de.fraunhofer.iosb.model.context.repository.local.impl.LocalFaaastRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.EdcException;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext.ERR_MSG_TEMPLATE;

public class LocalFaaastRepositoryClient extends LocalAasRepositoryClient<LocalFaaastRepositoryContext> {

    public LocalFaaastRepositoryClient(LocalFaaastRepositoryContext context) {
        super(context);
    }


    @Override
    public Environment getEnvironment() {
        return new DefaultEnvironment.Builder()
                .assetAdministrationShells(context.getAllAas())
                .submodels(context.getAllSubmodels())
                .conceptDescriptions(context.getAllConceptDescriptions())
                .build();
    }

    public <T extends EventMessage> SubscriptionId subscribeTo(Class<T> messageClass, Consumer<T> consumer) {
        try {
            List<Reference> references = context.getReferences();

            if (references.isEmpty()) {
                return context.subscribe(SubscriptionInfo.create(messageClass, consumer));
            }

            return context.subscribe(SubscriptionInfo.create(messageClass, consumer, references::contains));
        } catch (MessageBusException messageBusException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Subscribing to event", getUri()), messageBusException);
        }
    }

    public void unsubscribeFrom(SubscriptionId id) {
        try {
            context.unsubscribe(id);
        } catch (MessageBusException messageBusException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Unsubscribing from event", getUri()), messageBusException);
        }
    }

    @Override
    public List<Reference> getReferences() {
        return context.getReferences();
    }

    @Override
    public List<PolicyBinding> getPolicyBindings() {
        return context.getPolicyBindings();
    }
}
