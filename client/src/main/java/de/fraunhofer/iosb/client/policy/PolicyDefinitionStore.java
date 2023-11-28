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
package de.fraunhofer.iosb.client.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.spi.monitor.Monitor;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains user added PolicyDefinitions.
 */
class PolicyDefinitionStore {
    private final Map<String, PolicyDefinition> policyDefinitions;
    private final Monitor monitor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    PolicyDefinitionStore(Monitor monitor, String acceptedPolicyDefinitionsPath) {
        this.monitor = monitor;
        this.policyDefinitions = new ConcurrentHashMap<>();
        loadPolicyDefinitions(acceptedPolicyDefinitionsPath);
    }

    /**
     * Get all stored PolicyDefinitions.
     *
     * @return Stored PolicyDefinitions (non-null but possibly empty)
     */
    List<PolicyDefinition> getPolicyDefinitions() {
        return new ArrayList<>(policyDefinitions.values());
    }

    /**
     * Add PolicyDefinition to the store.
     *
     * @param newPolicyDefinitions PolicyDefinitions to be stored (non-null)
     */
    void putPolicyDefinitions(PolicyDefinition... newPolicyDefinitions) {
        Objects.requireNonNull(newPolicyDefinitions, "newPolicyDefinitions is null");
        for (PolicyDefinition newPolicyDefinition : newPolicyDefinitions) {
            if (!policyDefinitions.containsKey(newPolicyDefinition.getId())) {
                policyDefinitions.put(newPolicyDefinition.getId(), newPolicyDefinition);
            }
        }
    }

    /**
     * Remove a policy definition
     *
     * @param policyDefinitionId policyDefinition ID (non null)
     * @return Optional containing removed policy definition or null
     */
    Optional<PolicyDefinition> removePolicyDefinition(String policyDefinitionId) {
        Objects.requireNonNull(policyDefinitionId, "policyDefinitionId is null");
        return Optional.ofNullable(policyDefinitions.remove(policyDefinitionId));
    }

    /**
     * Update a policyDefinition
     *
     * @param policyDefinitionId PolicyDefinition ID (non null)
     * @param policyDefinition   The updated policyDefinition
     * @return Optional containing updated policy definition or null
     */
    Optional<PolicyDefinition> updatePolicyDefinitions(PolicyDefinition policyDefinition) {
        var policyDefinitionId = policyDefinition.getId();
        Objects.requireNonNull(policyDefinitionId, "contractOfferId is null");
        Objects.requireNonNull(policyDefinition, "contractOffer is null");
        if (policyDefinitions.containsKey(policyDefinitionId)) {
            return Optional.ofNullable(policyDefinitions.put(policyDefinitionId, policyDefinition));
        }
        return Optional.empty();
    }

    void loadPolicyDefinitions(String acceptedPolicyDefinitionsPath) {
        Path path;
        if (Objects.nonNull(acceptedPolicyDefinitionsPath)) {
            path = Path.of(acceptedPolicyDefinitionsPath);
            try {
                var acceptedPolicyDefinitions = objectMapper.readValue(path.toFile(),
                        PolicyDefinition[].class);
                putPolicyDefinitions(acceptedPolicyDefinitions);
            } catch (IOException loadAcceptedPolicyException) {
                monitor.warning(
                        format("[Client] Could not load accepted ContractOffers from %s",
                                acceptedPolicyDefinitionsPath),
                        loadAcceptedPolicyException);
            }
        }
    }
}
