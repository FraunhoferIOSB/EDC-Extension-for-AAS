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
package de.fraunhofer.iosb.ilt.dataspace.model.context.repository.local.impl;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.model.context.AasServerContext;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.MessageBus;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;

import java.net.URI;
import java.util.List;
import java.util.Objects;


/**
 * Context holding information specifically about a FA³ST service.
 */
public class LocalFaaastRepositoryContext extends AasServerContext {

    private final MessageBus<?> messageBus;
    private final Persistence<?> persistence;


    private LocalFaaastRepositoryContext(URI uri, MessageBus<?> messageBus,
                                         Persistence<?> persistence, List<PolicyBinding> policyBindings,
                                         boolean onlySubmodels) {
        super(uri, policyBindings, onlySubmodels, new NoAuth(), false);
        this.messageBus = messageBus;
        this.persistence = persistence;
    }


    /**
     * Returns the FA³ST message bus.
     *
     * @return the message bus.
     */
    public MessageBus<?> getMessageBus() {
        return messageBus;
    }


    /**
     * Returns the FA³ST persistence.
     *
     * @return the persistence.
     */
    public Persistence<?> getPersistence() {
        return persistence;
    }


    /**
     * Builder for {@link LocalFaaastRepositoryContext}.
     */
    public static class Builder extends AbstractBuilder<LocalFaaastRepositoryContext, Builder> {
        private MessageBus<?> messageBus;
        private Persistence<?> persistence;


        /** Default constructor. */
        public Builder() {}


        @Override
        protected Builder self() {
            return this;
        }


        /**
         * Sets the FA³ST message bus.
         *
         * @param messageBus the message bus.
         * @return this builder.
         */
        public Builder messageBus(MessageBus<?> messageBus) {
            this.messageBus = messageBus;
            return this;
        }


        /**
         * Sets the FA³ST persistence.
         *
         * @param persistence the persistence.
         * @return this builder.
         */
        public Builder persistence(Persistence<?> persistence) {
            this.persistence = persistence;
            return this;
        }


        /**
         * Builds the {@link LocalFaaastRepositoryContext} instance.
         *
         * @return the built context.
         */
        public LocalFaaastRepositoryContext build() {
            super.validate();
            Objects.requireNonNull(messageBus, "FA³ST MessageBus cannot be null");
            Objects.requireNonNull(persistence, "FA³ST Persistence cannot be null");

            return new LocalFaaastRepositoryContext(uri, messageBus, persistence, policyBindings,
                    onlySubmodels);
        }
    }
}
