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

import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AcceptedPolicyStoreTest {
    private static final String POLICY_DEFINITION_ID = "policy-definition-id";
    private static final String UNKNOWN_POLICY_DEFINITION_ID = "unknown-policy-definition-id";

    private AcceptedPolicyStore testSubject;


    @BeforeEach
    void setUp() {
        testSubject = new AcceptedPolicyStore();
    }


    @Test
    void getPolicyDefinitionsContainsAddedPolicyDefinitionTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        testSubject.addPolicyDefinition(policyDefinition);

        assertTrue(testSubject.getPolicyDefinitions().contains(policyDefinition));
    }


    @Test
    void getPolicyDefinitionsReturnsEmptyCollectionWhenNothingAddedTest() {
        assertTrue(testSubject.getPolicyDefinitions().isEmpty());
    }


    @Test
    void getPoliciesReturnsAssociatedPolicyTest() {
        var policy = mock(Policy.class);
        var policyDefinition = mock(PolicyDefinition.class);
        when(policyDefinition.getPolicy()).thenReturn(policy);
        testSubject.addPolicyDefinition(policyDefinition);

        assertTrue(testSubject.getPolicies().contains(policy));
    }


    @Test
    void getPoliciesReturnsEmptyCollectionWhenNothingAddedTest() {
        assertTrue(testSubject.getPolicies().isEmpty());
    }


    @Test
    void addPolicyDefinitionMakesDefinitionRetrievableTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        testSubject.addPolicyDefinition(policyDefinition);

        assertEquals(1, testSubject.getPolicyDefinitions().size());
        assertTrue(testSubject.getPolicyDefinitions().contains(policyDefinition));
    }


    @Test
    void addPolicyDefinitionIgnoresSameInstanceTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        testSubject.addPolicyDefinition(policyDefinition);
        testSubject.addPolicyDefinition(policyDefinition);

        assertEquals(1, testSubject.getPolicyDefinitions().size());
    }


    @Test
    void removePolicyDefinitionRemovesDefinitionTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        when(policyDefinition.getId()).thenReturn(POLICY_DEFINITION_ID);
        testSubject.addPolicyDefinition(policyDefinition);

        assertTrue(testSubject.removePolicyDefinition(POLICY_DEFINITION_ID));
        assertFalse(testSubject.getPolicyDefinitions().contains(policyDefinition));
    }


    @Test
    void removePolicyDefinitionReturnsFalseForUnknownIdTest() {
        var policyDefinition = mock(PolicyDefinition.class);
        when(policyDefinition.getId()).thenReturn(POLICY_DEFINITION_ID);
        testSubject.addPolicyDefinition(policyDefinition);

        assertFalse(testSubject.removePolicyDefinition(UNKNOWN_POLICY_DEFINITION_ID));
        assertEquals(1, testSubject.getPolicyDefinitions().size());
    }


    @Test
    void updatePolicyDefinitionAddsDefinitionTest() {
        var policy = mock(Policy.class);
        var policyDefinition = mock(PolicyDefinition.class);
        when(policyDefinition.getId()).thenReturn(POLICY_DEFINITION_ID);
        when(policyDefinition.getPolicy()).thenReturn(policy);

        assertTrue(testSubject.updatePolicyDefinition(policyDefinition));
        assertTrue(testSubject.getPolicyDefinitions().contains(policyDefinition));
    }


    @Test
    void updatePolicyDefinitionThrowsWhenPolicyDefinitionIdIsNullTest() {
        var exception = assertThrows(NullPointerException.class, () -> testSubject.updatePolicyDefinition(mock(PolicyDefinition.class)));

        assertEquals("policyDefinitionId", exception.getMessage());
    }
}
