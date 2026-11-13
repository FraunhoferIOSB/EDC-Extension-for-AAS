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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * Contains user added PolicyDefinitions.
 */
public class AcceptedPolicyStore {
    private final List<PolicyDefinition> policyDefinitions;

    public AcceptedPolicyStore() {
        this.policyDefinitions = new ArrayList<>();
    }


    /**
     * Returns a copy of the stored policies.
     *
     * @return Stored PolicyDefinitions (non-null but possibly empty)
     */
    Collection<PolicyDefinition> getPolicyDefinitions() {
        return List.copyOf(policyDefinitions);
    }


    /**
     * Returns a copy of the stored policies.
     *
     * @return Stored PolicyDefinitions (non-null but possibly empty)
     */
    Collection<Policy> getPolicies() {
        return List.copyOf(policyDefinitions.stream().map(PolicyDefinition::getPolicy).toList());
    }


    /**
     * Add policyDefinition to the store.
     *
     * @param policyDefinition policyDefinition to be stored (non-null)
     */
    void addPolicyDefinition(PolicyDefinition policyDefinition) {
        if (!policyDefinitions.contains(policyDefinition)) {
            policyDefinitions.add(policyDefinition);
        }
    }


    /**
     * Remove a policy definition
     *
     * @param policyDefinitionId policyDefinition ID
     */
    boolean removePolicyDefinition(String policyDefinitionId) {
        return policyDefinitions.removeIf(def -> def.getId().equals(policyDefinitionId));
    }


    /**
     * Update a policyDefinition
     *
     * @param policyDefinition The updated policyDefinition
     * @return true if the element was added in the process (upsert)
     */
    boolean updatePolicyDefinition(PolicyDefinition policyDefinition) {
        var policyDefinitionId = policyDefinition.getId();
        Objects.requireNonNull(policyDefinitionId, "policyDefinitionId");
        Objects.requireNonNull(policyDefinition.getPolicy(), "policy");
        int len = policyDefinitions.size();
        policyDefinitions.add(policyDefinition);
        return len != policyDefinitions.size();
    }
}
