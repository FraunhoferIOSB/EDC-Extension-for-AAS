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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class DefaultingParticipantContextStoreTest {
    private static final String DEFAULT_PARTICIPANT_CONTEXT_ID = "default-participant-context-id";
    private static final String PARTICIPANT_CONTEXT_ID = "participant-context-id";
    private static final String PARTICIPANT_CONTEXT_ID_KEY = "edc.participant.context.id";

    private ParticipantContextStore delegate;
    private Config config;
    private DefaultingParticipantContextStore testSubject;


    @BeforeEach
    void setUp() {
        delegate = mock(ParticipantContextStore.class);
        config = mock(Config.class);
        when(config.getString(PARTICIPANT_CONTEXT_ID_KEY, null)).thenReturn(DEFAULT_PARTICIPANT_CONTEXT_ID);
        testSubject = new DefaultingParticipantContextStore(delegate, config);
    }


    @Test
    void findByIdUsesProvidedIdInsteadOfDefaultTest() {
        var participantContext = mock(ParticipantContext.class);
        when(delegate.findById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success(participantContext));

        var result = testSubject.findById(PARTICIPANT_CONTEXT_ID);

        assertTrue(result.succeeded());
        assertEquals(participantContext, result.getContent());
        verify(delegate).findById(PARTICIPANT_CONTEXT_ID);
        verify(delegate, never()).findById(DEFAULT_PARTICIPANT_CONTEXT_ID);
    }


    @Test
    void findByIdFailsWhenNoDefaultParticipantContextIdConfiguredTest() {
        var configWithoutDefault = mock(Config.class);
        when(configWithoutDefault.getString(PARTICIPANT_CONTEXT_ID_KEY, null)).thenReturn(null);
        var storeWithoutDefault = new DefaultingParticipantContextStore(delegate, configWithoutDefault);

        var result = storeWithoutDefault.findById(null);

        assertTrue(result.failed());
        assertTrue(result.getFailureDetail().contains(PARTICIPANT_CONTEXT_ID_KEY));
    }


    @Test
    void queryReturnsDelegateSuccessTest() {
        var querySpec = mock(QuerySpec.class);
        var participantContext = mock(ParticipantContext.class);
        when(delegate.query(querySpec)).thenReturn(StoreResult.success(List.of(participantContext)));

        var result = testSubject.query(querySpec);

        assertTrue(result.succeeded());
        assertTrue(result.getContent().contains(participantContext));
        verify(delegate).query(querySpec);
    }


    @Test
    void queryReturnsDelegateFailureTest() {
        var querySpec = mock(QuerySpec.class);
        when(delegate.query(querySpec)).thenReturn(StoreResult.notFound("participant context not found"));

        var result = testSubject.query(querySpec);

        assertTrue(result.failed());
        verify(delegate).query(querySpec);
    }


    @Test
    void createReturnsDelegateSuccessTest() {
        var participantContext = mock(ParticipantContext.class);
        when(delegate.create(participantContext)).thenReturn(StoreResult.success(null));

        var result = testSubject.create(participantContext);

        assertTrue(result.succeeded());
        verify(delegate).create(participantContext);
    }


    @Test
    void createReturnsDelegateFailureTest() {
        var participantContext = mock(ParticipantContext.class);
        when(delegate.create(participantContext)).thenReturn(StoreResult.generalError("participant context already exists"));

        var result = testSubject.create(participantContext);

        assertTrue(result.failed());
        verify(delegate).create(participantContext);
    }


    @Test
    void updateReturnsDelegateSuccessTest() {
        var participantContext = mock(ParticipantContext.class);
        when(delegate.update(participantContext)).thenReturn(StoreResult.success(null));

        var result = testSubject.update(participantContext);

        assertTrue(result.succeeded());
        verify(delegate).update(participantContext);
    }


    @Test
    void updateReturnsDelegateFailureTest() {
        var participantContext = mock(ParticipantContext.class);
        when(delegate.update(participantContext)).thenReturn(StoreResult.generalError("participant context could not be updated"));

        var result = testSubject.update(participantContext);

        assertTrue(result.failed());
        verify(delegate).update(participantContext);
    }


    @Test
    void deleteByIdReturnsDelegateSuccessTest() {
        when(delegate.deleteById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.success(null));

        var result = testSubject.deleteById(PARTICIPANT_CONTEXT_ID);

        assertTrue(result.succeeded());
        verify(delegate).deleteById(PARTICIPANT_CONTEXT_ID);
    }


    @Test
    void deleteByIdReturnsDelegateFailureTest() {
        when(delegate.deleteById(PARTICIPANT_CONTEXT_ID)).thenReturn(StoreResult.generalError("participant context not found"));

        var result = testSubject.deleteById(PARTICIPANT_CONTEXT_ID);

        assertTrue(result.failed());
        verify(delegate).deleteById(PARTICIPANT_CONTEXT_ID);
    }
}
