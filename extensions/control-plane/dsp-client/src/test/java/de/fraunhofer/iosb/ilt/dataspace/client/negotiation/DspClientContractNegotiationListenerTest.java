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
package de.fraunhofer.iosb.ilt.dataspace.client.negotiation;

import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class DspClientContractNegotiationListenerTest {
    private static final String NEGOTIATION_ID = "negotiation-id";
    private static final String UNKNOWN_NEGOTIATION_ID = "unknown-negotiation-id";
    private static final String ERROR_DETAIL = "negotiation was declined";

    private DspClientContractNegotiationListener testSubject;
    private ContractNegotiation negotiation;


    @BeforeEach
    void setUp() {
        testSubject = new DspClientContractNegotiationListener();

        negotiation = mock(ContractNegotiation.class);
        when(negotiation.getId()).thenReturn(NEGOTIATION_ID);
        when(negotiation.getErrorDetail()).thenReturn(ERROR_DETAIL);
    }


    @Test
    void finalizedCompletesSubscribedFutureTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);

        testSubject.finalized(negotiation);

        assertTrue(future.isDone());
        assertEquals(negotiation, future.join());
    }


    @Test
    void finalizedDoesNotCompleteFutureForUnknownNegotiationTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);
        var unknownNegotiation = mock(ContractNegotiation.class);
        when(unknownNegotiation.getId()).thenReturn(UNKNOWN_NEGOTIATION_ID);

        testSubject.finalized(unknownNegotiation);

        assertFalse(future.isDone());
    }


    @Test
    void terminatedCompletesSubscribedFutureExceptionallyTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);

        testSubject.terminated(negotiation);

        assertTrue(future.isCompletedExceptionally());
        var exception = assertThrows(CompletionException.class, future::join);
        assertTrue(exception.getCause().getMessage().contains(NEGOTIATION_ID));
        assertTrue(exception.getCause().getMessage().contains(ERROR_DETAIL));
    }


    @Test
    void terminatedDoesNotCompleteFutureForUnknownNegotiationTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);
        var unknownNegotiation = mock(ContractNegotiation.class);
        when(unknownNegotiation.getId()).thenReturn(UNKNOWN_NEGOTIATION_ID);

        testSubject.terminated(unknownNegotiation);

        assertFalse(future.isDone());
    }


    @Test
    void addListenerReturnsPendingFutureCompletableByFinalizedTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);

        assertFalse(future.isDone());

        testSubject.finalized(negotiation);

        assertTrue(future.isDone());
        assertEquals(negotiation, future.join());
    }


    @Test
    void addListenerReplacesPreviouslyRegisteredFutureForSameIdTest() {
        var firstFuture = testSubject.addListener(NEGOTIATION_ID);
        var secondFuture = testSubject.addListener(NEGOTIATION_ID);

        assertNotSame(firstFuture, secondFuture);

        testSubject.finalized(negotiation);

        assertFalse(firstFuture.isDone());
        assertEquals(negotiation, secondFuture.join());
    }


    @Test
    void removeListenerPreventsSubscribedFutureFromBeingCompletedTest() {
        var future = testSubject.addListener(NEGOTIATION_ID);

        testSubject.removeListener(NEGOTIATION_ID);
        testSubject.finalized(negotiation);

        assertFalse(future.isDone());
    }


    @Test
    void removeListenerDoesNotThrowForUnknownNegotiationTest() {
        assertDoesNotThrow(() -> testSubject.removeListener(UNKNOWN_NEGOTIATION_ID));
    }
}
