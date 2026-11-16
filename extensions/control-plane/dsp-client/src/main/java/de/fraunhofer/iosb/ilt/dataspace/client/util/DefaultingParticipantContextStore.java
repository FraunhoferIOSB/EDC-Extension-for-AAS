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
package de.fraunhofer.iosb.ilt.dataspace.client.util;

import org.eclipse.edc.participantcontext.spi.store.ParticipantContextStore;
import org.eclipse.edc.participantcontext.spi.types.ParticipantContext;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Collection;


/**
 * Participant context store with a default participant context, given by the edc configuration value
 * "edc.participant.context.id".
 */
public class DefaultingParticipantContextStore implements ParticipantContextStore {
    private static final String PARTICIPANT_CONTEXT_ID_KEY = "edc.participant.context.id";
    private final ParticipantContextStore store;
    private final String defaultParticipantContextId;

    public DefaultingParticipantContextStore(ParticipantContextStore store, Config config) {
        this.store = store;
        this.defaultParticipantContextId = config.getString(PARTICIPANT_CONTEXT_ID_KEY, null);
    }


    @Override
    public StoreResult<ParticipantContext> findById(String participantContextId) {
        if (participantContextId != null) {
            return store.findById(participantContextId);
        }
        if (defaultParticipantContextId != null) {
            return store.findById(defaultParticipantContextId);
        }

        return StoreResult.generalError(String.format("Tried to get default participant context with no " +
                "configured participant context id: %s", PARTICIPANT_CONTEXT_ID_KEY));
    }


    @Override
    public StoreResult<Collection<ParticipantContext>> query(QuerySpec querySpec) {
        return store.query(querySpec);
    }


    @Override
    public StoreResult<Void> create(ParticipantContext participantContext) {
        return store.create(participantContext);
    }


    @Override
    public StoreResult<Void> update(ParticipantContext participantContext) {
        return store.update(participantContext);
    }


    @Override
    public StoreResult<Void> deleteById(String id) {
        return store.deleteById(id);
    }
}
