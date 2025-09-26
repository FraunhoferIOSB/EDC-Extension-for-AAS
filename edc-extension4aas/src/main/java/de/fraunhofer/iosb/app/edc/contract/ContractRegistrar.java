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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.AbstractResult;
import org.eclipse.edc.spi.result.Failure;
import org.eclipse.edc.spi.result.StoreFailure;
import org.eclipse.edc.spi.result.StoreResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.ACCESS_POLICY_FIELD;
import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.CONTRACT_POLICY_FIELD;
import static java.lang.String.format;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;

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
public class ContractRegistrar extends PipelineStep<ChangeSet<Asset, Asset>, Void> {

    public static final String DEFAULT_CONTRACT_POLICY_DEFINITION_ID = UUID.randomUUID().toString();
    public static final String DEFAULT_ACCESS_POLICY_DEFINITION_ID = UUID.randomUUID().toString();
    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final Configuration configuration;
    private final Monitor monitor;
    private final ObjectReader objectReader;
    private final String participantId;

    /**
     * Class constructor
     *
     * @param contractStore Add/remove EDC contracts.
     * @param policyStore   Add/remove EDC policies.
     * @param participantId Participant ID of this connector. Needed for contract policies.
     */
    public ContractRegistrar(ContractDefinitionStore contractStore, PolicyDefinitionStore policyStore,
                             Monitor monitor, String participantId) {
        this.contractDefinitionStore = Objects.requireNonNull(contractStore);
        this.policyDefinitionStore = Objects.requireNonNull(policyStore);
        this.monitor = monitor;

        configuration = Configuration.getInstance();
        objectReader = new ObjectMapper().readerFor(Policy.class);
        this.participantId = participantId;
        // In the future, this could be done with an additional extension.
        registerDefaultPolicies();
    }

    /**
     * Adds/removes contracts for the asset IDs in the changeSet.
     * Registers the given asset IDs to the default contract with the default access and contract policies.
     *
     * @param changeSet The asset IDs to add / remove contracts with
     * @return No content, possibly a PipelineFailure.
     */
    @Override
    public PipelineResult<Void> apply(ChangeSet<Asset, Asset> changeSet) {
        var addedResults = changeSet.toAdd().stream().map(this::registerWithContract).toList();
        var removedResults = changeSet.toRemove().stream().map(this::removeFromContracts).toList();

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
        return PipelineResult.success();
    }

    private StoreResult<Void> registerWithContract(Asset asset) {
        String accessPolicyId = getAccessPolicyIdOrDefault(asset);
        String contractPolicyId = getContractPolicyIdOrDefault(asset);

        if (Objects.isNull(policyDefinitionStore.findById(accessPolicyId))) {
            monitor.warning(String.format("AccessPolicyDefinitions with id %s not found.", accessPolicyId));
        }
        if (Objects.isNull(policyDefinitionStore.findById(contractPolicyId))) {
            monitor.warning(String.format("ContractPolicyDefinitions with id %s not found.", contractPolicyId));
        }

        Optional<ContractDefinition> maybeContract = findCorrespondingContract(asset);

        if (maybeContract.isPresent()) {
            ContractDefinition updatedContract = getContractDefinition(asset, maybeContract.get());
            return contractDefinitionStore.update(updatedContract);
        }

        return contractDefinitionStore.save(getContractDefinition(asset));
    }

    private Criterion getAssetIdCriterion(String assetId) {
        return Criterion.criterion(Asset.PROPERTY_ID, "=", assetId);
    }

    private void doThrow(Exception exception) {
        throw new EdcException(exception);
    }

    private StoreResult<Void> removeFromContracts(Asset asset) {
        String accessPolicyId = getAccessPolicyIdOrDefault(asset);
        String contractPolicyId = getContractPolicyIdOrDefault(asset);

        Criterion assetIdCriterion = getAssetIdCriterion(asset.getId());

        if (findCorrespondingContract(asset).isEmpty()) {
            return StoreResult.generalError(String.format("Contract for asset %s to remove not found, indicating an invalid state!", asset.getId()));
        }

        while (findCorrespondingContract(asset).isPresent()) {
            var contractToRemove = findCorrespondingContract(asset).get();

            if (contractToRemove.getAssetsSelector().size() != 1) {
                var updatedContract = ContractDefinition.Builder.newInstance()
                        .accessPolicyId(accessPolicyId)
                        .contractPolicyId(contractPolicyId)
                        .assetsSelector(contractToRemove.getAssetsSelector().stream()
                                .filter(criterion -> !criterion.equals(assetIdCriterion))
                                .toList())
                        .privateProperties(contractToRemove.getPrivateProperties())
                        .id(contractToRemove.getId())
                        .createdAt(contractToRemove.getCreatedAt())
                        .build();
                var updateResult = contractDefinitionStore.update(updatedContract);
                if (updateResult.succeeded()) {
                    continue;
                } else if (updateResult.failed() && updateResult.reason().equals(StoreFailure.Reason.NOT_FOUND)) {
                    monitor.warning(String.format("NOT_FOUND received when trying to update existing contract definition with id %s",
                            contractToRemove.getId()));
                    continue;
                }
                return StoreResult.generalError(updateResult.getFailureDetail());
            }

            // If the contract only contains this asset as assetsSelector, remove contract altogether.
            var deletionResult = contractDefinitionStore.deleteById(contractToRemove.getId());
            if (deletionResult.succeeded()) {
                continue;
            } else if (deletionResult.failed() && deletionResult.reason().equals(StoreFailure.Reason.NOT_FOUND)) {
                monitor.warning(String.format("NOT_FOUND received when trying to remove existing contract definition with id %s",
                        contractToRemove.getId()));
                continue;
            }
            return StoreResult.generalError(deletionResult.getFailureDetail());
        }
        return StoreResult.success();
    }

    private Optional<ContractDefinition> findCorrespondingContract(Asset asset) {
        String accessPolicyId = getAccessPolicyIdOrDefault(asset);
        String contractPolicyId = getContractPolicyIdOrDefault(asset);

        String opLeft = String.format("%s.%s", "assetsSelector", "operandLeft");
        String opRight = String.format("%s.%s", "assetsSelector", "operandRight");

        var searchQuery = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion("accessPolicyId", EQUAL, accessPolicyId))
                .filter(Criterion.criterion("contractPolicyId", EQUAL, contractPolicyId))
                .limit(1)
                .filter(Criterion.criterion(opLeft, EQUAL, asset.getId()))
                .filter(Criterion.criterion(opRight, EQUAL, Asset.PROPERTY_ID));

        return contractDefinitionStore.findAll(searchQuery.build())
                .findFirst();
    }


    private ContractDefinition getContractDefinition(Asset asset) {
        return ContractDefinition.Builder.newInstance()
                .accessPolicyId(getAccessPolicyIdOrDefault(asset))
                .contractPolicyId(getContractPolicyIdOrDefault(asset))
                .assetsSelectorCriterion(getAssetIdCriterion(asset.getId()))
                .privateProperty("creator", "AAS-Extension")
                .build();
    }

    private ContractDefinition getContractDefinition(Asset asset, ContractDefinition from) {
        return ContractDefinition.Builder.newInstance()
                .accessPolicyId(getAccessPolicyIdOrFrom(asset, from))
                .contractPolicyId(getContractPolicyIdOrFrom(asset, from))
                .assetsSelectorCriterion(getAssetIdCriterion(asset.getId()))
                .id(from.getId())
                .privateProperty("creator", "AAS-Extension")
                .build();
    }

    private void registerDefaultPolicies() {
        var defaultAccessPolicyPath = configuration.getDefaultAccessPolicyPath();
        var defaultContractPolicyPath = configuration.getDefaultContractPolicyPath();

        var defaultAccessPolicy = getPolicyDefinitionFromFile(defaultAccessPolicyPath)
                .orElse(initializeDefaultPolicy());
        var defaultContractPolicy = getPolicyDefinitionFromFile(defaultContractPolicyPath)
                .orElse(initializeDefaultPolicy());

        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_ACCESS_POLICY_DEFINITION_ID)
                .policy(defaultAccessPolicy)
                .build();
        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_CONTRACT_POLICY_DEFINITION_ID)
                .policy(defaultContractPolicy)
                .build();

        var accessPolicyResult = policyDefinitionStore.create(defaultAccessPolicyDefinition);
        var contractPolicyResult = policyDefinitionStore.create(defaultContractPolicyDefinition);

        if (accessPolicyResult.failed() && !ALREADY_EXISTS.equals(accessPolicyResult.reason())) {
            doThrow(new IllegalStateException(accessPolicyResult.getFailureDetail()));
        } else if (contractPolicyResult.failed() && !ALREADY_EXISTS.equals(contractPolicyResult.reason())) {
            doThrow(new IllegalStateException(contractPolicyResult.getFailureDetail()));
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

    private String getAccessPolicyIdOrDefault(Asset asset) {
        return (String) Optional
                .ofNullable(asset.getPrivateProperty(ACCESS_POLICY_FIELD))
                .orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID);
    }

    private String getAccessPolicyIdOrFrom(Asset asset, ContractDefinition from) {
        return (String) Optional
                .ofNullable(asset.getPrivateProperty(ACCESS_POLICY_FIELD))
                .orElse(from.getAccessPolicyId());
    }

    private String getContractPolicyIdOrFrom(Asset asset, ContractDefinition from) {
        return (String) Optional
                .ofNullable(asset.getPrivateProperty(CONTRACT_POLICY_FIELD))
                .orElse(from.getContractPolicyId());
    }

    private String getContractPolicyIdOrDefault(Asset asset) {
        return (String) Optional
                .ofNullable(asset.getPrivateProperty(CONTRACT_POLICY_FIELD))
                .orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID);
    }

    private Policy initializeDefaultPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type("USE")
                                .build())
                        .build())
                .assigner(participantId)
                .build();
    }

}
