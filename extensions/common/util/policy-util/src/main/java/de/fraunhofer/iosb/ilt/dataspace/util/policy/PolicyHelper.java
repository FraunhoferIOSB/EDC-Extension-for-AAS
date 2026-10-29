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
package de.fraunhofer.iosb.ilt.dataspace.util.policy;

import de.fraunhofer.iosb.ilt.dataspace.codec.Codec;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.StoreResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static de.fraunhofer.iosb.ilt.dataspace.constants.AasConstants.DEFAULT_POLICY_DEFINITION_ID;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;


/** Helper class for registering default access and usage policies. */
public abstract class PolicyHelper {

    private PolicyHelper() {}


    /**
     * Register default access and contract policies.
     *
     * @param codec Deserialize policy
     * @param policyDefinitionStore Store implementation to register policies.
     * @param participantId Used for the default policies.
     * @param policyPath Path to the access policy file, or null for default.
     */
    public static void registerDefaultPolicy(Codec codec, PolicyDefinitionStore policyDefinitionStore,
                                             String participantId, String policyPath) {

        Policy defaultPolicy = getPolicy(codec, participantId, policyPath);

        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_POLICY_DEFINITION_ID)
                .policy(defaultPolicy)
                .participantContextId(participantId)
                .build();

        StoreResult<PolicyDefinition> policyDefinitionStoreResult = policyDefinitionStore.create(defaultAccessPolicyDefinition);

        if (policyDefinitionStoreResult.failed() && ALREADY_EXISTS != policyDefinitionStoreResult.reason()) {
            throw new IllegalArgumentException(policyDefinitionStoreResult.getFailureDetail());
        }
    }


    private static Policy getPolicy(Codec codec, String participantId, String path) {
        Policy policy;
        if (path != null) {
            policy = getPolicyFromFile(codec, path).orElse(failure -> {
                throw new IllegalArgumentException(String.format("Could not parse default policy from %s: %s", path, failure.getFailureDetail()));
            });
        }
        else {
            policy = defaultPolicy(participantId);
        }
        return policy;
    }


    /**
     * Reads and deserializes a policy from a file.
     *
     * @param codec the codec used for deserialization.
     * @param filePath path to the policy file.
     * @return result containing the deserialized policy, or failure.
     */
    public static Result<Policy> getPolicyFromFile(Codec codec, String filePath) {
        try {
            return codec.deserialize(Files.readString(Path.of(filePath)), Policy.class);
        }
        catch (IOException ioException) {
            return Result.failure(ioException.getMessage());
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
