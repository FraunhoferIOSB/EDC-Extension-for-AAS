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
package de.fraunhofer.iosb.app.edc.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_ACCESS_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_CONTRACT_POLICY_DEFINITION_ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;

public abstract class PolicyHelper {

    private PolicyHelper() {
    }

    /**
     * Register default access and contract policies either by configuration variables or using internal (minimal) policies.
     *
     * @param monitor               monitor to log messages.
     * @param policyDefinitionStore Store implementation to register policies.
     * @param participantId         Used for the default policies.
     */
    public static void registerDefaultPolicies(Monitor monitor, PolicyDefinitionStore policyDefinitionStore, String participantId) {
        Configuration configuration = Configuration.getInstance();
        String accessPolicyPath = configuration.getDefaultAccessPolicyPath();
        String contractPolicyPath = configuration.getDefaultContractPolicyPath();

        Policy accessPolicy = getPolicyDefinitionFromFile(accessPolicyPath)
                .orElseGet(() -> {
                    monitor.severe(String.format("Could not find a valid policy at path %s. Using internal policy as default.", accessPolicyPath));
                    return defaultPolicy(participantId);
                });
        Policy contractPolicy = getPolicyDefinitionFromFile(contractPolicyPath)
                .orElseGet(() -> {
                    monitor.severe(String.format("Could not find a valid policy at path %s. Using internal policy as default.", contractPolicyPath));
                    return defaultPolicy(participantId);
                });

        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_ACCESS_POLICY_DEFINITION_ID)
                .policy(accessPolicy)
                .build();

        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_CONTRACT_POLICY_DEFINITION_ID)
                .policy(contractPolicy)
                .build();

        var accessPolicyResult = policyDefinitionStore.create(defaultAccessPolicyDefinition);
        var contractPolicyResult = policyDefinitionStore.create(defaultContractPolicyDefinition);

        if (accessPolicyResult.failed() && ALREADY_EXISTS != accessPolicyResult.reason()) {
            throw new IllegalArgumentException(accessPolicyResult.getFailureDetail());
        } else if (contractPolicyResult.failed() && ALREADY_EXISTS != contractPolicyResult.reason()) {
            throw new IllegalArgumentException(contractPolicyResult.getFailureDetail());
        }
    }

    private static Optional<Policy> getPolicyDefinitionFromFile(String filePath) {
        if (Objects.isNull(filePath)) {
            return Optional.empty();
        }

        try {
            Policy filePolicy = new ObjectMapper().readerFor(Policy.class).readValue(Path.of(filePath).toFile());
            return Optional.of(filePolicy);
        } catch (IOException ioException) {

            return Optional.empty();
        }
    }

    private static Policy defaultPolicy(String participantId) {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type(ODRL_USE_ACTION_ATTRIBUTE)
                                .build())
                        .build())
                .assigner(participantId)
                .build();
    }
}
