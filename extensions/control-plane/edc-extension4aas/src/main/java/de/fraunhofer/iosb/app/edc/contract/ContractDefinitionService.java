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

import de.fraunhofer.iosb.app.AasExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;


/**
 * Creates contracts, attaches asset IDs to existing contracts, removes asset IDs from contracts.
 */
public class ContractDefinitionService {

    public static final String ACCESS_POLICY_FIELD = "accessPolicyId";
    public static final String CONTRACT_POLICY_FIELD = "contractPolicyId";

    private final ContractDefinitionStore store;
    private final String participantId;


    /**
     * Class constructor.
     *
     * @param store Contract store of the EDC.
     * @param participantId Participant ID under which AAS extension registers data in data space
     */
    public ContractDefinitionService(ContractDefinitionStore store, String participantId) {
        this.store = store;
        this.participantId = participantId;
    }


    @SuppressWarnings("unchecked")
    private static List<String> getSelectedAssets(String assetId, Criterion assetsSelector) {
        List<String> selectedAssets;
        if (assetsSelector.getOperandRight() instanceof List<?> assets &&
                !assets.isEmpty() &&
                assets.get(0) instanceof String) {

            selectedAssets = new ArrayList<>((List<String>) assets);

        }
        else {
            throw new IllegalStateException("ContractDefinition created by AAS Extension was malformed");
        }

        if (!selectedAssets.contains(assetId)) {
            selectedAssets.add(assetId);
        }
        return selectedAssets;
    }


    /**
     * Adds the asset ID to the contract definition defined by the access&contract policy IDs. If no suitable contract definition exists, one will be created and stored to the EDC
     * contract definition store.
     *
     * @param assetId The asset id to be attached to the contract.
     * @param accessPolicyId The access policy of the contract definition.
     * @param contractPolicyId The contract (usage) policy of the contract definition.
     * @return Store result containing the state of the operation.
     */
    public StoreResult<Void> addToContractDefinition(String assetId, String accessPolicyId, String contractPolicyId) {
        Optional<ContractDefinition> maybeContract = findContracts(accessPolicyId, contractPolicyId).findFirst();

        if (maybeContract.isPresent()) {
            ContractDefinition updatedContract = addToExisting(assetId, maybeContract.get());

            return store.update(updatedContract);
        }

        return store.save(baseContractDefinition()
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .assetsSelectorCriterion(getAssetIdCriterion(assetId))
                .build());
    }


    /**
     * Removes the asset ID from the contract definition defined by the access&contract policy IDs. If multiple of such contract definition exist, the asset id will be removed from
     * all of them.
     *
     * @param assetId The asset id to be removed from any matching contract.
     * @param accessPolicyId The access policy of the contract definition.
     * @param contractPolicyId The contract (usage) policy of the contract definition.
     * @return Store result containing the state of the operation.
     */
    public StoreResult<Void> removeFromContract(String assetId, String accessPolicyId, String contractPolicyId) {
        List<String> problems = new ArrayList<>();

        var correspondingContracts = findCorrespondingContracts(accessPolicyId, contractPolicyId, assetId);

        for (ContractDefinition contractDefinition: correspondingContracts) {
            // Remove assetId from this contract definition
            Criterion assetsSelector = contractDefinition.getAssetsSelector().get(0);

            List<String> selectedAssets = getSelectedAssets(assetId, assetsSelector);
            List<String> updatedAssets = selectedAssets.stream()
                    .filter(s -> !assetId.equals(s))
                    .toList();

            Criterion updatedAssetsSelector = getAssetIdCriterion(updatedAssets);

            contractDefinition.getAssetsSelector().clear();
            contractDefinition.getAssetsSelector().add(updatedAssetsSelector);

            StoreResult<?> modifyResult;
            if (updatedAssets.isEmpty()) {
                modifyResult = store.deleteById(contractDefinition.getId());
            }
            else {
                modifyResult = store.update(contractDefinition);
            }

            if (modifyResult.failed() || (modifyResult.failed() && modifyResult.reason().equals(NOT_FOUND))) {
                problems.add(modifyResult.getFailureDetail());
            }
        }

        if (!problems.isEmpty()) {
            return StoreResult.generalError(problems.stream()
                    .filter(Objects::nonNull)
                    .reduce(String::concat)
                    .orElse("No error message."));
        }
        return StoreResult.success();
    }


    private ContractDefinition addToExisting(String assetId, ContractDefinition from) {
        // Contracts by this extension have exactly one AssetsSelectorCriterion.
        Criterion assetsSelector = from.getAssetsSelector().get(0);

        List<String> selectedAssets = getSelectedAssets(assetId, assetsSelector);

        Criterion updatedAssetsSelector = new Criterion(Asset.PROPERTY_ID, IN, selectedAssets);

        from.getAssetsSelector().clear();
        from.getAssetsSelector().add(updatedAssetsSelector);

        return from;
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


    private Stream<ContractDefinition> findContracts(String accessPolicyId, String contractPolicyId) {
        var searchQuery = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion(ACCESS_POLICY_FIELD, EQUAL, accessPolicyId))
                .filter(Criterion.criterion(CONTRACT_POLICY_FIELD, EQUAL, contractPolicyId))
                .filter(Criterion.criterion(format("privateProperties.'%screator'", EDC_NAMESPACE), EQUAL, AasExtension.NAME))
                .build();

        return store.findAll(searchQuery);
    }


    private Criterion getAssetIdCriterion(String assetId) {
        return Criterion.criterion(Asset.PROPERTY_ID, IN, List.of(assetId));
    }


    private Criterion getAssetIdCriterion(List<String> assetIds) {
        return Criterion.criterion(Asset.PROPERTY_ID, IN, assetIds);
    }


    private ContractDefinition.Builder baseContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .participantContextId(participantId)
                .privateProperty(EDC_NAMESPACE + "creator", AasExtension.NAME);
    }

}
