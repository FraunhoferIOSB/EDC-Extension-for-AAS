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
import de.fraunhofer.iosb.app.AasExtension;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
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
import org.eclipse.edc.spi.result.StoreFailure;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

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
public class ContractRegistrar {

    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final Configuration configuration;
    private final Monitor monitor;
    private final ObjectReader objectReader;
    private final Policy defaultPolicy;

    /**
     * Class constructor
     *
     * @param contractStore Add/remove EDC contracts.
     * @param policyStore   Add/remove EDC policies.
     */
    public ContractRegistrar(ContractDefinitionStore contractStore, PolicyDefinitionStore policyStore,
                             Monitor monitor) {
        this.contractDefinitionStore = Objects.requireNonNull(contractStore);
        this.policyDefinitionStore = Objects.requireNonNull(policyStore);
        this.monitor = monitor;

        defaultPolicy = initializeDefaultPolicy();

        configuration = Configuration.getInstance();
        objectReader = new ObjectMapper().readerFor(Policy.class);

        initializeContract();
    }

    public PipelineResult<Void> updateContract(List<String> toAdd, List<String> toRemove) {
        var contract =
                contractDefinitionStore.findAll(QuerySpec.max())
                        .filter(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator") != null)
                        .filter(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator").equals(AasExtension.ID))
                        .findFirst().orElseThrow(() -> new EdcException("Failed fetching ContractDefinition for Assets"));

        if (!toAdd.isEmpty() || !toRemove.isEmpty()) {
            var assetsSelector = contract.getAssetsSelector();
            assetsSelector.removeAll(toRemove.stream().map(createCriterion()).toList());
            assetsSelector.addAll(toAdd.stream().map(createCriterion()).toList());
            assetsSelector = assetsSelector.stream().distinct().toList();
            var updatedContract = ContractDefinition.Builder.newInstance()
                    .id(contract.getId())
                    .accessPolicyId(contract.getAccessPolicyId())
                    .contractPolicyId(contract.getContractPolicyId())
                    .assetsSelector(assetsSelector)
                    .privateProperties(contract.getPrivateProperties())
                    .build();

            return PipelineResult.from(contractDefinitionStore.update(updatedContract));
        }
        return PipelineResult.success(null);

    }

    private void initializeContract() {
        if (contractDefinitionStore.findAll(QuerySpec.max())
                .filter(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator") != null)
                .anyMatch(contractDefinition -> contractDefinition.getPrivateProperty(EDC_NAMESPACE + "creator").equals(AasExtension.ID))) {
            // Default contract has been created by another instance of this extension
            return;
        }

        var accessPolicyId = initializeAccessPolicyDefinition();
        var contractPolicyId = initializeContractPolicyDefinition();

        if (accessPolicyId.failed() || contractPolicyId.failed()) {
            throw new EdcException(String.format("Failed initializing default access/contract policies: %s; %s",
                    accessPolicyId.getFailureDetail(), contractPolicyId.getFailureDetail()));
        }

        var contract = ContractDefinition.Builder.newInstance()
                .accessPolicyId(accessPolicyId.getContent())
                .contractPolicyId(contractPolicyId.getContent())
                .privateProperty(EDC_NAMESPACE + "creator", AasExtension.ID)
                .build();

        var result = contractDefinitionStore.save(contract);

        if (result.failed() && !result.reason().equals(StoreFailure.Reason.ALREADY_EXISTS)) {
            throw new EdcException(String.format("Failed initializing default contract definition: %s",
                    result.getFailureDetail()));
        }
    }

    private static @NotNull Function<String, Criterion> createCriterion() {
        return id -> Criterion.criterion(Asset.PROPERTY_ID, "in", List.of(id));
    }

    private PipelineResult<String> initializeAccessPolicyDefinition() {
        var defaultAccessPolicyPath = configuration.getDefaultAccessPolicyPath();
        return initializePolicyDefinition(defaultAccessPolicyPath);
    }

    private PipelineResult<String> initializeContractPolicyDefinition() {
        var defaultContractPolicyPath = configuration.getDefaultContractPolicyPath();
        return initializePolicyDefinition(defaultContractPolicyPath);
    }

    private PipelineResult<String> initializePolicyDefinition(String path) {
        var defaultContractPolicy = getPolicyDefinitionFromFile(path).orElse(defaultPolicy);

        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(defaultContractPolicy)
                .build();

        var contractPolicyResult = policyDefinitionStore.create(defaultContractPolicyDefinition);

        if (contractPolicyResult.failed()) {
            return PipelineResult.from(contractPolicyResult).withContent(defaultContractPolicyDefinition.getId());
        }
        return PipelineResult.success(defaultContractPolicyDefinition.getId());
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
                        .action(Action.Builder.newInstance()
                                .type(ODRL_USE_ACTION_ATTRIBUTE)
                                .build())
                        .build())
                .assigner("provider")
                .build();
    }

}
