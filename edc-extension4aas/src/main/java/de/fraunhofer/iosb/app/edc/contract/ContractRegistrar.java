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
import de.fraunhofer.iosb.app.model.ChangeSet;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.pipeline.PipelineFailure;
import de.fraunhofer.iosb.app.pipeline.PipelineResult;
import de.fraunhofer.iosb.app.pipeline.PipelineStep;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    }

    /**
     * Adds/removes contracts for the asset IDs in the changeSet.
     * Registers the given asset IDs to the default contract with the default access and contract policies.
     *
     * @param changeSet The asset IDs to add / remove contracts with
     * @return No content, possibly a PipelineFailure.
     */
    @Override
    public PipelineResult<Void> apply(ChangeSet<String, String> changeSet) {
        var addedResults = changeSet.toAdd().stream().map(this::createDefaultContract).toList();
        var removedResults = changeSet.toRemove().stream().map(this::removeContract).toList();

        if (addedResults.stream().anyMatch(AbstractResult::failed) || removedResults.stream().anyMatch(AbstractResult::failed)) {
            var addedFailureMessages = addedResults.stream()
                    .filter(AbstractResult::failed)
                    .map(AbstractResult::getFailure)
                    .map(Failure::getMessages)
                    .flatMap(List::stream)
                    .collect(Collectors.toCollection(ArrayList::new));

            var removedFailureMessages = removedResults.stream()
                    .filter(AbstractResult::failed)
                    .map(AbstractResult::getFailure)
                    .map(Failure::getMessages)
                    .flatMap(List::stream)
                    .toList();

            addedFailureMessages.addAll(removedFailureMessages);
            return PipelineResult.failure(PipelineFailure.warning(addedFailureMessages));
        }
        return PipelineResult.success(null);
    }

    private PipelineResult<Void> createDefaultContract(String assetId) {
        var defaultAccessPolicyPath = configuration.getDefaultAccessPolicyPath();
        var defaultContractPolicyPath = configuration.getDefaultContractPolicyPath();

        var defaultAccessPolicy = getPolicyDefinitionFromFile(defaultAccessPolicyPath).orElse(defaultPolicy);
        var defaultContractPolicy = getPolicyDefinitionFromFile(defaultContractPolicyPath).orElse(defaultPolicy);

        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(defaultAccessPolicy.withTarget(assetId))
                .build();
        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .policy(defaultContractPolicy.withTarget(assetId))
                .build();

        var accessPolicyResult = policyDefinitionStore.create(defaultAccessPolicyDefinition);
        var contractPolicyResult = policyDefinitionStore.create(defaultContractPolicyDefinition);

        if (accessPolicyResult.failed()) {
            return PipelineResult.from(accessPolicyResult).withContent(null);
        } else if (contractPolicyResult.failed()) {
            return PipelineResult.from(contractPolicyResult).withContent(null);
        }

        var defaultContractDefinition = ContractDefinition.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .accessPolicyId(defaultAccessPolicyDefinition.getId())
                .contractPolicyId(defaultContractPolicyDefinition.getId())
                .assetsSelectorCriterion(Criterion.criterion(Asset.PROPERTY_ID, "=", assetId))
                .build();

        return PipelineResult.from(contractDefinitionStore.save(defaultContractDefinition));
    }

    private PipelineResult<Void> removeContract(String assetId) {
        var assetFilterExpression = new Criterion(Asset.PROPERTY_ID, "=", assetId);
        var queryAssetFilter = QuerySpec.Builder.newInstance().filter(List.of(assetFilterExpression)).build();

        var removeResult = contractDefinitionStore.findAll(queryAssetFilter)
                .map(contract -> contractDefinitionStore.deleteById(contract.getId()))
                .filter(AbstractResult::failed).toList();

        if (!removeResult.isEmpty()) {
            // Not being able to remove a contract can be considered a fatal error
            return PipelineResult.failure(
                    PipelineFailure.fatal(
                            removeResult.stream()
                                    .filter(AbstractResult::failed)
                                    .map(AbstractResult::getFailureMessages)
                                    .flatMap(List::stream)
                                    .toList()));
        }

        return PipelineResult.success(null);
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
                                .type("USE")
                                .build())
                        .build())
                .assigner("provider")
                .build();
    }

}
