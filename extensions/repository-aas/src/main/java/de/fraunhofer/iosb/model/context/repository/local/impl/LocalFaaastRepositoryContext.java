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
package de.fraunhofer.iosb.model.context.repository.local.impl;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.MessageBus;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.modifier.QueryModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingInfo;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.PersistenceException;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionId;
import de.fraunhofer.iosb.ilt.faaast.service.model.messagebus.SubscriptionInfo;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.spi.EdcException;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext.ERR_MSG_TEMPLATE;


public class LocalFaaastRepositoryContext extends AasRepositoryContext {

    private final MessageBus<?> messageBus;
    private final Persistence<?> persistence;


    private LocalFaaastRepositoryContext(URI uri, MessageBus<?> messageBus, Persistence<?> persistence, List<PolicyBinding> policyBindings, boolean onlySubmodels) {
        super(uri, policyBindings, onlySubmodels);
        this.messageBus = messageBus;
        this.persistence = persistence;
    }


    public SubscriptionId subscribe(SubscriptionInfo subscriptionInfo) throws MessageBusException {
        return messageBus.subscribe(subscriptionInfo);
    }


    public void unsubscribe(SubscriptionId id) throws MessageBusException {
        messageBus.unsubscribe(id);
    }


    private <I extends Identifiable> I getById(KeyTypes type, String id) {
        try {
            return (I) switch (type) {
                case ASSET_ADMINISTRATION_SHELL -> persistence.getAssetAdministrationShell(id, QueryModifier.DEFAULT);
                case SUBMODEL -> persistence.getSubmodel(id, QueryModifier.DEFAULT);
                case CONCEPT_DESCRIPTION -> persistence.getConceptDescription(id, QueryModifier.DEFAULT);
                default -> throw new IllegalArgumentException("Reference starts with non-identifiable.");
            };

        }
        catch (PersistenceException | ResourceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    public List<AssetAdministrationShell> getAllAas() {
        try {
            return persistence.getAllAssetAdministrationShells(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        }
        catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all AAS", getUri()), persistenceException);
        }
    }


    public List<Submodel> getAllSubmodels() {
        try {
            return persistence.getAllSubmodels(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        }
        catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all Submdoels", getUri()), persistenceException);
        }
    }


    public List<ConceptDescription> getAllConceptDescriptions() {
        try {
            return persistence.getAllConceptDescriptions(QueryModifier.DEFAULT, PagingInfo.ALL).getContent();
        }
        catch (PersistenceException persistenceException) {
            throw new EdcException(String.format(ERR_MSG_TEMPLATE, "Getting all ConceptDescriptions", getUri()), persistenceException);
        }
    }


    public static class Builder extends AbstractBuilder<LocalFaaastRepositoryContext, Builder> {
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


        public LocalFaaastRepositoryContext build() {
            super.validate();
            Objects.requireNonNull(messageBus, "FA³ST MessageBus cannot be null");
            Objects.requireNonNull(persistence, "FA³ST Persistence cannot be null");

            return new LocalFaaastRepositoryContext(uri, messageBus, persistence, policyBindings, onlySubmodels);
        }
    }
}
