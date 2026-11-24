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
package de.fraunhofer.iosb.ilt.dataspace.client.policy;

import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static de.fraunhofer.iosb.ilt.dataspace.client.DspClientController.MISSING_REQUEST_BODY_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class PolicyControllerTest {
    private static final String POLICY_DEFINITION_ID = "policy-definition-id";


    private AcceptedPolicyStore acceptedPolicyStore;
    private PolicyController testSubject;


    @BeforeEach
    void setUp() {
        acceptedPolicyStore = mock(AcceptedPolicyStore.class);
        testSubject = new PolicyController(acceptedPolicyStore);
    }


    @Test
    void getAcceptedPolicyDefinitionsReturnsPolicyDefinitionsFromStoreTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        var policyDefinitions = List.of(policyDefinition);
        when(acceptedPolicyStore.getPolicyDefinitions()).thenReturn(policyDefinitions);

        var result = testSubject.getAcceptedPolicyDefinitions();

        assertEquals(policyDefinitions, result);
    }


    @Test
    void getAcceptedPolicyDefinitionsReturnsEmptyCollectionTest() {
        when(acceptedPolicyStore.getPolicyDefinitions()).thenReturn(List.of());

        var result = testSubject.getAcceptedPolicyDefinitions();

        assertTrue(result.isEmpty());
    }


    @Test
    void addAcceptedPolicyDefinitionAddsPolicyDefinitionToStoreTest() {
        var policyDefinition = mock(PolicyDefinition.class);

        assertDoesNotThrow(() -> testSubject.addAcceptedPolicyDefinition(policyDefinition));

        verify(acceptedPolicyStore).addPolicyDefinition(policyDefinition);
    }


    @Test
    void addAcceptedPolicyDefinitionThrowsWhenPolicyDefinitionMissingTest() {
        var exception = assertThrows(InvalidRequestException.class, () -> testSubject.addAcceptedPolicyDefinition(null));

        assertEquals(MISSING_REQUEST_BODY_MESSAGE.formatted("policyDefinition"), exception.getMessage());
        verify(acceptedPolicyStore, never()).addPolicyDefinition(any());
    }


    @Test
    void deleteAcceptedPolicyDefinitionThrowsObjectNotFoundWhenRemovalFailsTest() {
        when(acceptedPolicyStore.removePolicyDefinition(POLICY_DEFINITION_ID)).thenReturn(false);

        assertThrows(ObjectNotFoundException.class, () -> testSubject.deleteAcceptedPolicyDefinition(POLICY_DEFINITION_ID));
    }


    @Test
    void deleteAcceptedPolicyDefinitionDoesNothingWhenRemovalSucceedsTest() {
        when(acceptedPolicyStore.removePolicyDefinition(POLICY_DEFINITION_ID)).thenReturn(true);

        assertDoesNotThrow(() -> testSubject.deleteAcceptedPolicyDefinition(POLICY_DEFINITION_ID));

        verify(acceptedPolicyStore).removePolicyDefinition(POLICY_DEFINITION_ID);
    }


    @Test
    void updateAcceptedPolicyDefinitionReturnsCreatedWhenUpdateSucceedsTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        when(policyDefinition.getId()).thenReturn(POLICY_DEFINITION_ID);
        when(acceptedPolicyStore.updatePolicyDefinition(policyDefinition)).thenReturn(true);

        Response response = testSubject.updateAcceptedPolicyDefinition(policyDefinition);

        assertEquals(201, response.getStatus());
        assertEquals(URI.create(POLICY_DEFINITION_ID), response.getLocation());
    }


    @Test
    void updateAcceptedPolicyDefinitionThrowsWhenPolicyDefinitionMissingTest() {
        var exception = assertThrows(InvalidRequestException.class, () -> testSubject.updateAcceptedPolicyDefinition(null));

        assertEquals(String.format(MISSING_REQUEST_BODY_MESSAGE, "PolicyDefinition"), exception.getMessage());
        verify(acceptedPolicyStore, never()).updatePolicyDefinition(any());
    }
}
