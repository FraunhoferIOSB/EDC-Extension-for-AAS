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
import com.fasterxml.jackson.databind.ObjectReader;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import de.fraunhofer.iosb.app.sync.ChangeSet;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Handle interactions with the ContractDefinitionStore, PolicyDefinitionStore.
 * Assigns EDC assets to EDC contracts.
 * <p>
 * There are two types of policies: AccessPolicies and ContractPolicies. Both
 * can be passed as files via the configuration. If no policies are passed,
 * USE permissions are used as default policies. For more info regarding
 * the difference between Access-/Contract-Policy, see
 * {@link ContractDefinition} documentation.
 */
public class ContractRegistrar extends PipelineStep<ChangeSet<String, String>, Void> {

    private static final String DEFAULT_ACCESS_POLICY_UID = "DEFAULT_ACCESS_POLICY";
    private static final String DEFAULT_CONTRACT_POLICY_UID = "DEFAULT_CONTRACT_POLICY";
    private static final String DEFAULT_CONTRACT_DEFINITION_UID = "DEFAULT_CONTRACT";
    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final Configuration configuration;
    private final Monitor monitor;
    private final ObjectReader objectReader;
    private final Policy defaultPolicy;
    private long contractNumber = 0L;

    /**
     * Creates an instance of the ContractHandler class.
     *
     * @param contractStore Needed to manage EDC contracts.
     * @param policyStore   Needed to manage EDC policies.
     */
    public ContractRegistrar(ContractDefinitionStore contractStore, PolicyDefinitionStore policyStore, Monitor monitor) {
        Objects.requireNonNull(contractStore, "ContractDefinitionStore");
        Objects.requireNonNull(policyStore, "PolicyDefinitionStore");
        this.contractDefinitionStore = contractStore;
        this.policyDefinitionStore = policyStore;
        this.monitor = monitor;

        defaultPolicy = initializeDefaultPolicy();

        configuration = Configuration.getInstance();
        objectReader = new ObjectMapper().readerFor(Policy.class);
    }

    /**
     * Registers the given assetId to the default contract with the default access and contract policies.
     * TODO FIX DOC
     *
     * @param stringStringChangeSet The asset ID.
     * @return Contract id of contract this assetId was registered to.
     */
    @Override
    public PipelineResult<Void> execute(ChangeSet<String, String> stringStringChangeSet) throws Exception {
        stringStringChangeSet.toAdd().forEach(this::createDefaultContract);
        stringStringChangeSet.toRemove().forEach(this::removeContract);
        return PipelineResult.success(null);
    }

    private void createDefaultContract(String assetId) {
        contractNumber++;
        var accessPolicyId = DEFAULT_ACCESS_POLICY_UID + contractNumber;
        var contractPolicyId = DEFAULT_CONTRACT_POLICY_UID + contractNumber;
        var contractDefinitionId = DEFAULT_CONTRACT_DEFINITION_UID + contractNumber;

        var defaultAccessPolicyPath = configuration.getDefaultAccessPolicyPath();
        var defaultContractPolicyPath = configuration.getDefaultContractPolicyPath();

        var defaultAccessPolicy = getPolicyDefinitionFromFile(defaultAccessPolicyPath).orElse(defaultPolicy);
        var defaultContractPolicy = getPolicyDefinitionFromFile(defaultContractPolicyPath).orElse(defaultPolicy);

        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(accessPolicyId)
                .policy(defaultAccessPolicy.withTarget(assetId))
                .build();
        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(contractPolicyId)
                .policy(defaultContractPolicy.withTarget(assetId))
                .build();

        policyDefinitionStore.create(defaultAccessPolicyDefinition);
        policyDefinitionStore.create(defaultContractPolicyDefinition);

        var defaultContractDefinition = ContractDefinition.Builder.newInstance()
                .id(contractDefinitionId)
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .assetsSelectorCriterion(Criterion.criterion(Asset.PROPERTY_ID, "=", assetId))
                .build();

        contractDefinitionStore.save(defaultContractDefinition);
    }

    private void removeContract(String assetId) {
        var assetFilterExpression = new Criterion(Asset.PROPERTY_ID, "=", assetId);
        var queryAssetFilter = QuerySpec.Builder.newInstance().filter(List.of(assetFilterExpression)).build();

        var failures = contractDefinitionStore.findAll(queryAssetFilter)
                .map(contract -> contractDefinitionStore.deleteById(contract.getId()))
                .filter(AbstractResult::failed)
                .map(AbstractResult::getFailureMessages)
                .flatMap(Collection::stream)
                .toList();
        if (!failures.isEmpty()) {
            throw new EdcException("Could not delete contracts for %s. %s".formatted(assetId, failures.toArray()));
        }

    }


    private Optional<Policy> getPolicyDefinitionFromFile(String filePath) {
        if (Objects.isNull(filePath)) {
            return Optional.empty();
        }

        try {
            Policy filePolicy = objectReader.readValue(Path.of(filePath).toFile());
            return Optional.of(filePolicy);
        } catch (IOException ioException) {
            monitor.severe(
                    format("Could not find a valid policy at path %s. Using internal default policy.",
                            filePath),
                    ioException);
            return Optional.empty();
        }
    }

    private Policy initializeDefaultPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .assigner("provider") // as this is just an example policy, assigner can be generic
                .build();
    }

}
