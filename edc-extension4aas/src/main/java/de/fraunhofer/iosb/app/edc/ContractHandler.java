package de.fraunhofer.iosb.app.edc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.dataspaceconnector.policy.model.Action;
import org.eclipse.dataspaceconnector.policy.model.Permission;
import org.eclipse.dataspaceconnector.policy.model.Policy;
import org.eclipse.dataspaceconnector.policy.model.Prohibition;
import org.eclipse.dataspaceconnector.spi.asset.AssetSelectorExpression;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.PolicyDefinition;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.eclipse.dataspaceconnector.spi.query.Criterion;
import org.eclipse.dataspaceconnector.spi.query.QuerySpec;
import org.eclipse.dataspaceconnector.spi.types.domain.contract.offer.ContractDefinition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import static java.lang.String.format;

/**
 * Handle interactions with the ContractDefinitionStore, PolicyDefinitionStore.
 * Assigns EDC assets to EDC contracts.
 * 
 * There are two types of policies: AccessPolicies and ContractPolicies. Both
 * can be passed as files via the configuration. If no policies are passed,
 * default USE permissions are put as default policies. For more info regarding
 * the difference between Access-/Contract-Policy, see
 * {@link ContractDefinition} documentation.
 */
public class ContractHandler {

    private static final String ASSET_PROPERTY_ID = "asset:prop:id";
    private static final String DEFAULT_ACCESS_POLICY_UID = "DEFAULT_ACCESS_POLICY";
    private static final String DEFAULT_CONTRACT_POLICY_UID = "DEFAULT_CONTRACT_POLICY";
    private static final String DEFAULT_CONTRACT_DEFINITION_UID = "DEFAULT_CONTRACT";
    private static final String EQUALS = "=";
    private long contractNumber = 0L;

    private final ContractDefinitionStore contractDefinitionStore;
    private final PolicyDefinitionStore policyDefinitionStore;
    private final Configuration configuration;
    private final Logger logger;

    /**
     * Initializes default policies / contract.

     * @param contractStore Contract store used to handle contract operations
     * @param policyStore   Policy store used to handle policy operations
     */
    public ContractHandler(@NonNull final ContractDefinitionStore contractStore,
            @NonNull final PolicyDefinitionStore policyStore) {
        this.contractDefinitionStore = contractStore;
        this.policyDefinitionStore = policyStore;

        configuration = Configuration.getInstance();
        logger = Logger.getInstance();

        initDefaultPolicyDefinitions();
        initDefaultContractDefinition();
    }

    private void initDefaultPolicyDefinitions() {
        final var defaultAccessPolicyDefinitionPath = configuration.getDefaultAccessPolicyDefinitionPath();
        final var defaultContractPolicyDefinitionPath = configuration.getDefaultContractPolicyDefinitionPath();

        final var usePermissionPolicy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .build();
        // Testing prohibitions
        final var denyProhibitionPolicy = Policy.Builder.newInstance()
                .prohibition(Prohibition.Builder.newInstance()
                        .action(Action.Builder.newInstance().type("DENY")
                                .build())
                        .build())
                .build();
        var defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_ACCESS_POLICY_UID)
                .policy(denyProhibitionPolicy)
                .build();
        var defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .id(DEFAULT_CONTRACT_POLICY_UID)
                .policy(usePermissionPolicy)
                .build();

        if (Objects.nonNull(defaultAccessPolicyDefinitionPath)) {
            // An access policy path has been provided via the configuration
            try {
                final var defaultAccessPolicy = new ObjectMapper().readValue(
                        Path.of(defaultAccessPolicyDefinitionPath).toFile(), Policy.class);
                defaultAccessPolicyDefinition = PolicyDefinition.Builder.newInstance()
                        .id(DEFAULT_ACCESS_POLICY_UID)
                        .policy(defaultAccessPolicy)
                        .build();
            } catch (IOException ioException) {
                logger.error("Could not find access policy at path" + defaultAccessPolicyDefinitionPath,
                        ioException);
            }

            // logger.log(new
            // ObjectMapper().writeValue(defaultAccessPolicyDefinition.getPolicy()));
        }

        if (Objects.nonNull(defaultContractPolicyDefinitionPath)) {
            // A contract policy path has been provided via the configuration
            try {
                final var defaultContractPolicy = new ObjectMapper()
                        .readValue(Path.of(defaultContractPolicyDefinitionPath).toFile(),
                                Policy.class);
                defaultContractPolicyDefinition = PolicyDefinition.Builder.newInstance()
                        .id(DEFAULT_CONTRACT_POLICY_UID)
                        .policy(defaultContractPolicy)
                        .build();
            } catch (IOException ioException) {
                logger.error("Could not find contract policy at path" +
                        defaultAccessPolicyDefinitionPath,
                        ioException);
            }
        }

        policyDefinitionStore.save(defaultAccessPolicyDefinition);
        policyDefinitionStore.save(defaultContractPolicyDefinition);
    }

    private void initDefaultContractDefinition() {
        final var defaultContractDefinition = ContractDefinition.Builder.newInstance()
                .id(DEFAULT_CONTRACT_DEFINITION_UID)
                .accessPolicyId(DEFAULT_ACCESS_POLICY_UID)
                .contractPolicyId(DEFAULT_CONTRACT_POLICY_UID)
                .selectorExpression(
                        AssetSelectorExpression.Builder.newInstance()
                                .whenEquals(ASSET_PROPERTY_ID, null)
                                .build())
                .build();
        contractDefinitionStore.save(defaultContractDefinition);
    }

    /**
     * Registers the asset to the contract, if a contract with this contract ID
     * exists in the ContractDefinitionStore of the EDC.
     * Asset ID will not be validated!

     * @param assetId    The asset ID
     * @param contractId The contract ID
     * @return The contract id
     */
    public String registerAssetToContract(final String assetId, final String contractId) {
        final var contractDefinition = contractDefinitionStore.findById(contractId);

        if (Objects.isNull(contractDefinition)) {
            return null;
        }

        var newContractDefinition = deepCopyContractDefinition(contractDefinition, contractId, assetId);
        newContractDefinition.getSelectorExpression().getCriteria()
                .removeIf(crit -> crit.getOperandLeft().equals(ASSET_PROPERTY_ID) ||
                            crit.getOperandRight().equals(ASSET_PROPERTY_ID));

        newContractDefinition.getSelectorExpression().getCriteria()
                .add(new Criterion(ASSET_PROPERTY_ID, EQUALS, assetId));

        contractDefinitionStore.save(newContractDefinition);
        return newContractDefinition.getId();
    }

    /**
     * Registers the asset to the default contract.
     * Asset ID will not be validated!

     * @param assetId The asset ID
     * 
     * @return Contract id of contract this asset was registered to
     */
    public String registerAssetToDefaultContract(final String assetId) {
        return registerAssetToContract(assetId, DEFAULT_CONTRACT_DEFINITION_UID);
    }

    /**
     * Unregisters this asset from any contract linked to it.

     * @param assetId Asset ID
     */
    public void unregisterAssetFromAllContracts(final String assetId) {
        var assetFilter = format("asset:prop:id = %s", assetId);

        var queryAssetFilter = QuerySpec.Builder.newInstance().filter(assetFilter).build();

        contractDefinitionStore.findAll(queryAssetFilter)
                .forEach(contract -> unregisterAssetFromContract(assetId, contract.getId()));
    }

    /**
     * Unregisters this asset from this contract.

     * @param assetId    Asset ID
     * @param contractId Contract ID
     */
    public void unregisterAssetFromContract(final String assetId, final String contractId) {
        var criteria = contractDefinitionStore.findById(contractId).getSelectorExpression().getCriteria();
        criteria.removeIf(criterion -> assetId.equals(criterion.getOperandRight()));
    }

    /**
     * Adds a policy definition to the EDC PolicyDefinitionStore.

     * @param policyDefinition PolicyDefinition to be added
     * @return The UID of the newly added policy definition
     */
    public String addPolicyDefinition(final PolicyDefinition policyDefinition) {
        policyDefinitionStore.save(policyDefinition);
        return policyDefinition.getUid();
    }

    /**
     * Deletes a policy definition from the EDC PolicyDefinitionStore.

     * @param policyDefinitionId PolicyDefinition to be deleted
     * @return The removed policy definition or null if the policy definition was
     *         not found
     */
    public PolicyDefinition deletePolicyDefinition(final String policyDefinitionId) {
        return policyDefinitionStore.deleteById(policyDefinitionId);
    }

    /**
     * Adds a new contract definition containing no linked asset ID.
     * AccessPolicyUID, ContractPolicyUID are not validated!

     * @param contractDefinitionUid       A new UID for this contract definition
     * @param accessPolicyDefinitionUid   Access policy definition UID
     * @param contractPolicyDefinitionUid Contract policy definition UID
     * @return The UID of the newly added contract definition
     */
    public String addContractDefinition(final String contractDefinitionUid, final String accessPolicyDefinitionUid,
            final String contractPolicyDefinitionUid) {
        final var newContractDefinition = ContractDefinition.Builder.newInstance()
                .id(contractDefinitionUid)
                .accessPolicyId(accessPolicyDefinitionUid)
                .contractPolicyId(contractPolicyDefinitionUid)
                .selectorExpression(
                        AssetSelectorExpression.Builder.newInstance()
                                .whenEquals(ASSET_PROPERTY_ID, null)
                                .build())
                .build();
        contractDefinitionStore.save(newContractDefinition);
        return contractDefinitionUid;
    }

    /**
     * Adds a new contract provided by the user

     * @param contractSerialized       The provided contract (JSON)
     * @return The UID of the newly added contract definition
     */
    public String addContractDefinition(final String contractSerialized) throws JsonProcessingException {
        var newContractDefinition = new ObjectMapper().readValue(
                contractSerialized, ContractDefinition.class);
        contractDefinitionStore.save(newContractDefinition);
        return newContractDefinition.getId();
    }

    /**
     * Delete a contract if every associated asset has been removed

     * @param contractId Contract to be deleted
     */
    public void deleteContractReference(final String contractId, final String assetId) {
        contractDefinitionStore.findById(contractId).getSelectorExpression().getCriteria()
                .removeIf(crit -> assetId.equals(crit.getOperandRight()));
    }

    /**
     * Delete a contract

     * @param contractId Contract to be deleted
     * @return The removed contract definition or null if the contract definition
     *         was not found
     */
    public ContractDefinition deleteContractDefinition(final String contractId) {
        return contractDefinitionStore.deleteById(contractId);
    }

    private ContractDefinition deepCopyContractDefinition(ContractDefinition toCopy, String contractId,
            String newAssetId) {
        var localContractNumber = contractNumber++;
        // Copy access policy
        var oldAccessPolicy = policyDefinitionStore.findById(toCopy.getAccessPolicyId());
        var newAccessPolicy = oldAccessPolicy.getPolicy().withTarget(newAssetId);
        var accessPolicyId = oldAccessPolicy.getUid().concat(String.valueOf(localContractNumber));
        policyDefinitionStore.save(PolicyDefinition.Builder.newInstance().policy(newAccessPolicy)
                .id(accessPolicyId).build());
        // Copy contract policy
        var oldContractPolicy = policyDefinitionStore.findById(toCopy.getContractPolicyId());
        var newContractPolicy = oldContractPolicy.getPolicy().withTarget(newAssetId);
        var contractPolicyId = oldContractPolicy.getUid().concat(String.valueOf(localContractNumber));
        policyDefinitionStore.save(PolicyDefinition.Builder.newInstance().policy(newContractPolicy)
                .id(contractPolicyId).build());
        // Copy selector expression
        var selectorExpression = AssetSelectorExpression.Builder.newInstance().build();
        toCopy.getSelectorExpression().getCriteria().forEach(criterion -> selectorExpression.getCriteria()
                .add(new Criterion(criterion.getOperandLeft(), criterion.getOperator(),
                        criterion.getOperandRight())));

        return ContractDefinition.Builder.newInstance()
                .id(contractId.concat(String.valueOf(localContractNumber)))
                .accessPolicyId(accessPolicyId)
                .contractPolicyId(contractPolicyId)
                .selectorExpression(selectorExpression).build();
    }
}
