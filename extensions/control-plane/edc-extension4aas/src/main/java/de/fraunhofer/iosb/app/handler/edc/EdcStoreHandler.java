package de.fraunhofer.iosb.app.handler.edc;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.AasExtension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_ACCESS_POLICY_DEFINITION_ID;
import static de.fraunhofer.iosb.constants.AasConstants.DEFAULT_CONTRACT_POLICY_DEFINITION_ID;
import static java.lang.String.format;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.EQUAL;
import static org.eclipse.edc.spi.query.CriterionOperatorRegistry.IN;
import static org.eclipse.edc.spi.result.StoreFailure.Reason.NOT_FOUND;

public class EdcStoreHandler {
    public static final String ACCESS_POLICY_FIELD = "accessPolicyId";
    public static final String CONTRACT_POLICY_FIELD = "contractPolicyId";

    private final AssetIndex assetIndex;
    private final ContractDefinitionStore contractDefinitionStore;

    public EdcStoreHandler(AssetIndex assetIndex, ContractDefinitionStore contractDefinitionStore) {
        this.assetIndex = assetIndex;
        this.contractDefinitionStore = contractDefinitionStore;
    }

    public StoreResult<Void> register(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> assetCreationResult = assetIndex.create(asset);

        if (assetCreationResult.succeeded()) {
            return registerWithContract(policyBinding, asset);
        }

        return assetCreationResult;
    }

    public StoreResult<Void> unregister(PolicyBinding policyBinding, String assetId) {
        StoreResult<Asset> assetDeleteResult = assetIndex.deleteById(assetId);

        if (assetDeleteResult.succeeded()) {
            return removeFromContract(policyBinding, assetId);
        }

        return StoreResult.generalError(assetDeleteResult.getFailure().getFailureDetail());
    }

    public StoreResult<Asset> update(Asset asset) {
        return assetIndex.updateAsset(asset);
    }

    private StoreResult<Void> removeFromContract(PolicyBinding policyBinding, String assetId) {
        List<String> problems = new ArrayList<>();

        String accessPolicyId = policyBinding.accessPolicyDefinitionId();
        String contractPolicyId = policyBinding.contractPolicyDefinitionId();

        var correspondingContracts = findCorrespondingContracts(accessPolicyId, contractPolicyId, assetId);

        for (ContractDefinition contractDefinition : correspondingContracts) {
            var updatedContract = getBaseContractDefinition()
                    .id(contractDefinition.getId())
                    .accessPolicyId(contractDefinition.getAccessPolicyId())
                    .contractPolicyId(contractDefinition.getContractPolicyId())
                    .assetsSelector(contractDefinition.getAssetsSelector().stream().map(
                            predicate -> predicate.getOperandLeft().equals(Asset.PROPERTY_ID) && predicate.getOperator().equals(IN) ?
                                    Criterion.criterion(predicate.getOperandLeft(), predicate.getOperator(),
                                            ((List<?>) predicate.getOperandRight()).stream()
                                                    .filter(id -> !assetId.equals(id))
                                                    .toList()) :
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

    private StoreResult<Void> registerWithContract(PolicyBinding policyBinding, Asset asset) {

        String accessPolicyId = Optional.ofNullable(policyBinding.accessPolicyDefinitionId())
                .orElse(DEFAULT_ACCESS_POLICY_DEFINITION_ID);
        String contractPolicyId = Optional.ofNullable(policyBinding.contractPolicyDefinitionId())
                .orElse(DEFAULT_CONTRACT_POLICY_DEFINITION_ID);

        Optional<ContractDefinition> maybeContract = findContracts(accessPolicyId, contractPolicyId).findFirst();

        if (maybeContract.isPresent()) {
            ContractDefinition updatedContract = getContractDefinition(asset, maybeContract.get());

            return contractDefinitionStore.update(updatedContract);
        }

        return contractDefinitionStore.save(getBaseContractDefinition()
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .assetsSelectorCriterion(getAssetIdCriterion(asset.getId()))
                .build());
    }

    private Criterion getAssetIdCriterion(String assetId) {
        return Criterion.criterion(Asset.PROPERTY_ID, IN, List.of(assetId));
    }

    private @NotNull Stream<ContractDefinition> findContracts(String accessPolicyId, String contractPolicyId) {
        var searchQuery = QuerySpec.Builder.newInstance()
                .filter(Criterion.criterion(ACCESS_POLICY_FIELD, EQUAL, accessPolicyId))
                .filter(Criterion.criterion(CONTRACT_POLICY_FIELD, EQUAL, contractPolicyId))
                .filter(Criterion.criterion(format("privateProperties.'%screator'", EDC_NAMESPACE), EQUAL, AasExtension.NAME))
                .build();

        return contractDefinitionStore.findAll(searchQuery);
    }


    @SuppressWarnings("unchecked")
    private ContractDefinition getContractDefinition(Asset asset, ContractDefinition from) {
        // Contracts by this extension have exactly one AssetsSelectorCriterion.
        Criterion assetsSelector = from.getAssetsSelector().get(0);

        List<String> selectedAssets;
        if (assetsSelector.getOperandRight() instanceof Collection<?> assets &&
                !assets.isEmpty() &&
                assets.stream().allMatch(elem -> elem instanceof String)) {

            selectedAssets = new ArrayList<>((Collection<String>) assets);

        } else {
            throw new IllegalStateException("ContractDefinition created by AAS Extension was malformed");
        }

        if (!selectedAssets.contains(asset.getId())) {
            selectedAssets.add(asset.getId());
        }
        Criterion updatedAssetsSelector = new Criterion(Asset.PROPERTY_ID, IN, selectedAssets);

        return getBaseContractDefinition()
                .accessPolicyId(from.getAccessPolicyId())
                .contractPolicyId(from.getContractPolicyId())
                .assetsSelectorCriterion(updatedAssetsSelector)
                .id(from.getId())
                .build();
    }


    private ContractDefinition.Builder getBaseContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .privateProperty(EDC_NAMESPACE + "creator", AasExtension.NAME);
    }

}
