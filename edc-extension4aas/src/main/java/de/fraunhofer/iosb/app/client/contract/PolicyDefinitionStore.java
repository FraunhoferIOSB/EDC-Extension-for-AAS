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
package de.fraunhofer.iosb.app.client.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contains user added PolicyDefinitions.
 */
public class PolicyDefinitionStore {
    private final Map<String, PolicyDefinition> policyDefinitions;
    private static final Logger LOGGER = Logger.getInstance();

    public PolicyDefinitionStore() {
        this.policyDefinitions = new ConcurrentHashMap<>();
        loadPolicyDefinitions(Configuration.getInstance());
    }

    /**
     * Get all stored PolicyDefinitions.
     *
     * @return Stored PolicyDefinitions (non-null but possibly empty)
     */
    public List<PolicyDefinition> getPolicyDefinitions() {
        return new ArrayList<>(policyDefinitions.values());
    }

    /**
     * Add PolicyDefinition to the store.
     *
     * @param newPolicyDefinitions PolicyDefinitions to be stored (non-null)
     */
    public void putPolicyDefinitions(PolicyDefinition... newPolicyDefinitions) {
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
    public Optional<PolicyDefinition> removePolicyDefinition(String policyDefinitionId) {
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
    public Optional<PolicyDefinition> updatePolicyDefinitions(String policyDefinitionId, PolicyDefinition policyDefinition) {
        Objects.requireNonNull(policyDefinitionId, "contractOfferId is null");
        Objects.requireNonNull(policyDefinition, "contractOffer is null");
        if (policyDefinitions.containsKey(policyDefinitionId)) {
            return Optional.ofNullable(policyDefinitions.put(policyDefinitionId, policyDefinition));
        }
        return Optional.empty();
    }

    private void loadPolicyDefinitions(Configuration config) {
        if (Objects.isNull(config.getAcceptedPolicyDefinitionsPath())) {
            return;
        }
        var acceptedPolicyDefinitionsPath = Path.of(config.getAcceptedPolicyDefinitionsPath());
        try {
            var mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            var acceptedPolicyDefinitions = mapper.readValue(acceptedPolicyDefinitionsPath.toFile(),
                    PolicyDefinition[].class);
            putPolicyDefinitions(acceptedPolicyDefinitions);
        } catch (IOException e) {
            LOGGER.warn("[Client] Could not load accepted ContractOffers (edc.ids.client.acceptedContractOfferPaths)",
                    e);
        }
    }
}
