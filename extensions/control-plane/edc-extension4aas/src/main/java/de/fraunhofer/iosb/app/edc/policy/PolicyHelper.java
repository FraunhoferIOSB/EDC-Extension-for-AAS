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
package de.fraunhofer.iosb.app.edc.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.StoreResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_POLICY_DEFINITION_ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;


public abstract class PolicyHelper {

    private PolicyHelper() {
    }


    /**
     * Register default access and contract policies either by configuration variables or using internal (minimal) policies.
     *
     * @param monitor monitor to log messages.
     * @param policyDefinitionStore Store implementation to register policies.
     * @param participantId Used for the default policies.
     */
    public static void registerDefaultPolicies(Monitor monitor, PolicyDefinitionStore policyDefinitionStore, String participantId) {
        Configuration configuration = Configuration.getInstance();

        Policy defaultPolicy = getPolicy(monitor, participantId, configuration.getDefaultAccessPolicyPath());

        var defaultPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_POLICY_DEFINITION_ID)
                .policy(defaultPolicy)
                .build();

        StoreResult<PolicyDefinition> storeResult = policyDefinitionStore.create(defaultPolicyDefinition);

        if (storeResult.failed() && ALREADY_EXISTS != storeResult.reason()) {
            throw new IllegalArgumentException(storeResult.getFailureDetail());
        }
    }


    private static Policy getPolicy(Monitor monitor, String participantId, String path) {
        Policy policy;
        if (path != null) {
            policy = getPolicyDefinitionFromFile(monitor, path).orElse(defaultPolicy(participantId));
        }
        else {
            policy = defaultPolicy(participantId);
        }
        return policy;
    }


    private static Optional<Policy> getPolicyDefinitionFromFile(Monitor monitor, String filePath) {
        try {
            Policy filePolicy = new ObjectMapper().readerFor(Policy.class).readValue(Path.of(filePath).toFile());
            return Optional.of(filePolicy);
        }
        catch (IOException ioException) {
            monitor.severe(String.format("Could not find a valid policy at path %s. Using internal policy as default.", filePath));
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
