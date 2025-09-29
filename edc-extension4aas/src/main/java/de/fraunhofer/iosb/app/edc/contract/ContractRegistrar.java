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
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.ACCESS_POLICY_FIELD;
import static de.fraunhofer.iosb.app.aas.mapper.environment.EnvironmentToAssetMapper.CONTRACT_POLICY_FIELD;
import static java.lang.String.format;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.ALREADY_EXISTS;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

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
        String accessPolicyId = getAccessPolicy(asset).orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID);
        String contractPolicyId = getContractPolicy(asset).orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID);

        if (Objects.isNull(policyDefinitionStore.findById(accessPolicyId))) {
            monitor.warning(String.format("AccessPolicyDefinition with id %s not found.", accessPolicyId));
        }
        if (Objects.isNull(policyDefinitionStore.findById(contractPolicyId))) {
            monitor.warning(String.format("ContractPolicyDefinition with id %s not found.", contractPolicyId));
        }

        Optional<ContractDefinition> maybeContract = findContracts(accessPolicyId, contractPolicyId).findFirst();

        if (maybeContract.isPresent()) {
            ContractDefinition updatedContract = getContractDefinition(asset, maybeContract.get());
            return contractDefinitionStore.update(updatedContract);
        }

        return contractDefinitionStore.save(getBaseContractDefinition()
                .accessPolicyId(getAccessPolicy(asset).orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID))
                .contractPolicyId(getContractPolicy(asset).orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID))
                .assetsSelectorCriterion(getAssetIdCriterion(asset.getId()))
                .build()
        );
    }

    private StoreResult<Void> removeFromContracts(Asset asset) {
        String accessPolicyId = getAccessPolicy(asset).orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID);
        String contractPolicyId = getContractPolicy(asset).orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID);

        var correspondingContracts = findCorrespondingContracts(accessPolicyId, contractPolicyId, asset.getId());

        for (ContractDefinition contractDefinition : correspondingContracts) {
            var updatedContract = getBaseContractDefinition()
                    .id(contractDefinition.getId())
                    .accessPolicyId(contractDefinition.getAccessPolicyId())
                    .contractPolicyId(contractDefinition.getContractPolicyId())
                    .assetsSelector(contractDefinition.getAssetsSelector().stream().map(
                            predicate -> predicate.getOperandLeft().equals(Asset.PROPERTY_ID) && predicate.getOperator().equals(IN) ?
                                    Criterion.criterion(predicate.getOperandLeft(), predicate.getOperator(),
                                            ((List<?>) predicate.getOperandRight()).stream().filter(assetId -> !asset.getId().equals(assetId)).toList()) :
                                    predicate
                    ).toList())
                    .privateProperties(contractDefinition.getPrivateProperties())
                    .build();

            // Tie up loose ends
            StoreResult<?> modifyResult;
            if (((List<?>) updatedContract.getAssetsSelector().get(0).getOperandRight()).isEmpty()) {
                modifyResult = contractDefinitionStore.deleteById(updatedContract.getId());
            } else {
                modifyResult = contractDefinitionStore.update(updatedContract);
            }

            if (modifyResult.succeeded()) {
                continue;
            } else if (modifyResult.failed() && modifyResult.reason().equals(NOT_FOUND)) {
                monitor.warning(String.format("%s received when trying to update existing contract definition with id %s",
                        NOT_FOUND, contractDefinition.getId()));
                continue;
            }
            return StoreResult.generalError(modifyResult.getFailureDetail());
        }
        return StoreResult.success();
    }

    private List<ContractDefinition> findCorrespondingContracts(String accessPolicyId, String contractPolicyId, String assetId) {
        Stream<ContractDefinition> contractDefinitions = findContracts(accessPolicyId, contractPolicyId);

        // ContainsPredicate does not seem to work, so implement it here
        return contractDefinitions.filter(
                contractDefinition -> contractDefinition.getAssetsSelector()
                        .stream().filter(predicate -> predicate.getOperandLeft().equals(Asset.PROPERTY_ID))
                        .filter(predicate -> predicate.getOperator().equalsIgnoreCase(IN))
                        .anyMatch(predicate -> ((List<?>) predicate.getOperandRight()).contains(assetId))
        ).toList();
    }

    private @NotNull Stream<ContractDefinition> findContracts(String accessPolicyId, String contractPolicyId) {
        var searchQuery = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion(ACCESS_POLICY_FIELD, EQUAL, accessPolicyId))
                .filter(Criterion.criterion(CONTRACT_POLICY_FIELD, EQUAL, contractPolicyId))
                .filter(Criterion.criterion("privateProperties.creator", EQUAL, AasExtension.NAME))
                .build();

        return contractDefinitionStore.findAll(searchQuery);
    }

    private Criterion getAssetIdCriterion(String assetId) {
        return Criterion.criterion(Asset.PROPERTY_ID, IN, List.of(assetId));
    }

    @SuppressWarnings("unchecked")
    private ContractDefinition getContractDefinition(Asset asset, ContractDefinition from) {
        // Contracts by this extension have exactly one AssetsSelectorCriterion.
        Criterion assetsSelector = from.getAssetsSelector().get(0);

        List<String> selectedAssets = List.of();
        if (assetsSelector.getOperandRight() instanceof Collection<?> assets &&
                !assets.isEmpty() &&
                assets.stream().allMatch(elem -> elem instanceof String)) {

            selectedAssets = new ArrayList<>((Collection<String>) assets);

        } else {
            doThrow(new IllegalStateException("ContractDefinition created by AAS Extension was malformed"));
        }

        if (!selectedAssets.contains(asset.getId())) {
            selectedAssets.add(asset.getId());
        }
        Criterion updatedAssetsSelector = new Criterion(Asset.PROPERTY_ID, IN, selectedAssets);

        return getBaseContractDefinition()
                .accessPolicyId(getAccessPolicy(asset).orElse(from.getAccessPolicyId()))
                .contractPolicyId(getContractPolicy(asset).orElse(from.getAccessPolicyId()))
                .assetsSelectorCriterion(updatedAssetsSelector)
                .id(from.getId())
                .build();
    }

    private ContractDefinition.Builder getBaseContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .privateProperty("creator", AasExtension.NAME);
    }

    private void registerDefaultPolicies() {
        var defaultAccessPolicyPath = configuration.getDefaultAccessPolicyPath();
        var defaultContractPolicyPath = configuration.getDefaultContractPolicyPath();

        var defaultAccessPolicy = getPolicyDefinitionFromFile(defaultAccessPolicyPath)
                .orElse(defaultPolicy());
        var defaultContractPolicy = getPolicyDefinitionFromFile(defaultContractPolicyPath)
                .orElse(defaultPolicy());

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

    private Optional<String> getAccessPolicy(Asset asset) {
        return Optional.ofNullable(asset.getPrivateProperty(ACCESS_POLICY_FIELD).toString());
    }

    private Optional<String> getContractPolicy(Asset asset) {
        return Optional.ofNullable(asset.getPrivateProperty(CONTRACT_POLICY_FIELD).toString());
    }

    private Policy defaultPolicy() {
        return Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance()
                                .type(ODRL_USE_ACTION_ATTRIBUTE)
                                .build())
                        .build())
                .assigner(participantId)
                .build();
    }

    private void doThrow(Exception exception) {
        throw new EdcException(exception);
    }
}
