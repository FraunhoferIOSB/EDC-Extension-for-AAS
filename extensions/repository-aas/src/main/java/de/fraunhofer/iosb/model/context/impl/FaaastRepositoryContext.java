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
package de.fraunhofer.iosb.model.context.impl;

import de.fraunhofer.iosb.client.exception.NotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.MessageBus;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingInfo;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.EventMessage;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionId;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionInfo;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.util.ReferenceHelper;
import de.fraunhofer.iosb.model.context.AasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.Key;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Referable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.spi.EdcException;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes.EXTERNAL_REFERENCE;

public class FaaastRepositoryContext extends AasRepositoryContext {
    private static final String ERR_MSG_TEMPLATE = "%s from %s failed.";

    private final MessageBus<?> messageBus;
    private final Persistence<?> persistence;


    private FaaastRepositoryContext(URI uri, MessageBus<?> messageBus, Persistence<?> persistence) {
        super(uri);
        this.messageBus = messageBus;
        this.persistence = persistence;
    }

    public <T extends EventMessage> SubscriptionId subscribeTo(Class<T> messageClass, Consumer<T> consumer) {
        try {
            return messageBus.subscribe(SubscriptionInfo.create(messageClass, consumer));
        } catch (MessageBusException messageBusException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Subscribing to event", getUri()), messageBusException);
        }
    }

    public void unsubscribeFrom(SubscriptionId id) {
        try {
            messageBus.unsubscribe(id);
        } catch (MessageBusException messageBusException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Unsubscribing from event", getUri()), messageBusException);
        }
    }

    /**
     * For a given reference, get the referable from this FA³ST repository. Can be an identifiable as well.
     *
     * @param reference The reference for the referable to get.
     * @param clazz     The actual class of the reference.
     * @param <R>       Type of the referable.
     * @return The referable referenced by the reference.
     */
    @SuppressWarnings("unchecked")
    public <R extends Referable> R getReferable(Reference reference, Class<R> clazz) throws NotFoundException {
        if (EXTERNAL_REFERENCE == ReferenceHelper.determineReferenceType(reference)) {
            throw new IllegalArgumentException("Cannot get referable by external reference.");
        }

        if (reference.getKeys().isEmpty()) {
            throw new NotFoundException("Reference has no keys.");
        }

        Key root = ReferenceHelper.getRoot(reference);
        KeyTypes rootType = root.getType();
        String rootId = root.getValue();

        var superElement = getById(rootType, rootId);

        if (reference.getKeys().size() == 1) {
            // unchecked: Identifiable is a direct implementor of Referable
            return (R) superElement;
        }

        if (superElement instanceof Submodel submodel) {
            // We know that nested references can only be submodel elements.
            return AasUtils.resolve(reference, new DefaultEnvironment.Builder().submodels(submodel).build(), clazz);
        }

        throw new IllegalArgumentException(String.format("Reference malformed: %s", ReferenceHelper.toString(reference)));
    }

    private <I extends Identifiable> I getById(KeyTypes type, String id) {
        try {
            return (I) switch (type) {
                case ASSET_ADMINISTRATION_SHELL -> persistence.getAssetAdministrationShell(id, QueryModifier.DEFAULT);
                case SUBMODEL -> persistence.getSubmodel(id, QueryModifier.DEFAULT);
                case CONCEPT_DESCRIPTION -> persistence.getConceptDescription(id, QueryModifier.DEFAULT);
                default -> throw new IllegalArgumentException("Reference starts with non-identifiable.");
            };

        } catch (PersistenceException | ResourceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AssetAdministrationShell> getAllAas() {
        try {
            return persistence.getAllAssetAdministrationShells(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        } catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all AAS", getUri()), persistenceException);
        }
    }

    public List<Submodel> getAllSubmodels() {
        try {
            return persistence.getAllSubmodels(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        } catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all Submdoels", getUri()), persistenceException);
        }
    }

    public List<ConceptDescription> getAllConceptDescriptions() {
        try {
            return persistence.getAllConceptDescriptions(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        } catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all ConceptDescriptions", getUri()), persistenceException);
        }
    }

    public static class Builder extends AbstractBuilder<FaaastRepositoryContext, Builder> {
        private MessageBus<?> messageBus;
        private Persistence<?> persistence;

        public Builder() {
        }

        public Builder messageBus(MessageBus<?> messageBus) {
            this.messageBus = messageBus;
            return this;
        }

        public Builder persistence(Persistence<?> persistence) {
            this.persistence = persistence;
            return this;
        }

        public FaaastRepositoryContext build() {
            super.validate();
            Objects.requireNonNull(messageBus, "FA³ST MessageBus cannot be null");
            Objects.requireNonNull(persistence, "FA³ST Persistence cannot be null");

            return new FaaastRepositoryContext(uri, messageBus, persistence);
        }
    }
}
