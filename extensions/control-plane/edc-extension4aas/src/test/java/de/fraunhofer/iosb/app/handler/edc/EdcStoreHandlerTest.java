package de.fraunhofer.iosb.app.handler.edc;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.client.repository.remote.impl.RemoteAasRepositoryClient;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getSubmodel;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdcStoreHandlerTest {

    private EdcStoreHandler testSubject;
    private InMemoryAssetIndex assetIndex;
    private InMemoryContractDefinitionStore contractDefinitionStore;

    private final CriterionOperatorRegistry criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();

    private final IdentifiableMapper identifiableMapper =
            new IdentifiableMapper(new RemoteAasRepositoryClient(new RemoteAasRepositoryContext.Builder()
                    .uri(URI.create("http://example.com"))
                    .build()));

    @BeforeEach
    void setUp() {
        assetIndex = new InMemoryAssetIndex(criterionOperatorRegistry);
        contractDefinitionStore = new InMemoryContractDefinitionStore(criterionOperatorRegistry);
        testSubject = new EdcStoreHandler(assetIndex, contractDefinitionStore);
    }

    @Test
    void register_withNormalValues_shouldSucceed() {
        Submodel submodel = getSubmodel();
        Asset asset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        PolicyBinding policyBinding = PolicyBinding.ofDefaults(reference);
        assertRegister(policyBinding, asset);
    }

    @Test
    void unregister_previouslyRegistered_shouldSucceed() {
        Submodel submodel = getSubmodel();
        Asset asset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        PolicyBinding policyBinding = PolicyBinding.ofDefaults(reference);

        assertRegister(policyBinding, asset);

        assertUnregisterToEmpty(policyBinding, asset.getId());
    }

    @Test
    void unregister_oneOfRegistered_shouldRetainContract() {
        Submodel submodel = getSubmodel();
        Asset anAsset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        PolicyBinding policyBinding = PolicyBinding.ofDefaults(reference);

        assertRegister(policyBinding, anAsset);

        Asset anotherAsset = identifiableMapper.map(getSubmodel());
        StoreResult<Void> result = testSubject.register(policyBinding, anotherAsset);
        assertTrue(result.succeeded());

        assertEquals(2, assetIndex.countAssets(List.of()));
        assertEquals(anotherAsset, assetIndex.findById(anotherAsset.getId()));

        assertAdditionToContractDefinition(
                policyBinding.accessPolicyDefinitionId(),
                policyBinding.contractPolicyDefinitionId(),
                anotherAsset.getId());

        assertUnregisterToNotEmpty(policyBinding, anAsset.getId());
    }

    @Test
    void update_existingAsset_shouldSucceed() {
        Submodel submodel = getSubmodel();
        Asset asset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        PolicyBinding policyBinding = PolicyBinding.ofDefaults(reference);

        assertRegister(policyBinding, asset);
        Asset assetUpdated = asset.toBuilder()
                .property(AAS_V30_NAMESPACE.concat("modelingType"), "NewProperty")
                .build();

        StoreResult<Asset> result = testSubject.update(assetUpdated);
        assertTrue(result.succeeded());

        Asset assetIs = assetIndex.findById(asset.getId());
        assertEquals(assetUpdated, assetIs);

        assertSingleContractDefinition(
                policyBinding.accessPolicyDefinitionId(),
                policyBinding.contractPolicyDefinitionId(),
                asset.getId());
    }

    @Test
    void update_inexistentAsset_shouldNotAddIt() {
        Submodel submodel = getSubmodel();
        Asset asset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        PolicyBinding policyBinding = PolicyBinding.ofDefaults(reference);

        // Intentionally commented out. Do not register this asset.
        // assertRegister(policyBinding, asset);
        Asset assetUpdated = asset.toBuilder()
                .property(AAS_V30_NAMESPACE.concat("modelingType"), "NewProperty")
                .build();

        StoreResult<Asset> result = testSubject.update(assetUpdated);
        assertTrue(result.failed());

        Asset assetIs = assetIndex.findById(asset.getId());
        assertNull(assetIs);

        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertTrue(contractDefinitions.isEmpty());
    }

    private void assertRegister(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> result = testSubject.register(policyBinding, asset);

        assertTrue(result.succeeded());

        assertEquals(1, assetIndex.countAssets(List.of()));
        assertEquals(asset, assetIndex.findById(asset.getId()));

        assertSingleContractDefinition(
                policyBinding.accessPolicyDefinitionId(),
                policyBinding.contractPolicyDefinitionId(),
                asset.getId());
    }

    private void assertUnregisterToNotEmpty(PolicyBinding policyBinding, String assetId) {
        StoreResult<Void> result = testSubject.unregister(policyBinding, assetId);

        assertTrue(result.succeeded());

        assertNotEquals(0, assetIndex.countAssets(List.of()));

        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertEquals(1, contractDefinitions.size());
    }

    private void assertUnregisterToEmpty(PolicyBinding policyBinding, String assetId) {
        StoreResult<Void> result = testSubject.unregister(policyBinding, assetId);

        assertTrue(result.succeeded());

        assertEquals(0, assetIndex.countAssets(List.of()));

        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertEquals(0, contractDefinitions.size());
    }

    private void assertSingleContractDefinition(String accessPolicyId, String contractPolicyId, String assetId) {
        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertEquals(1, contractDefinitions.size());
        ContractDefinition contractDefinition = contractDefinitions.get(0);
        assertEquals(accessPolicyId, contractDefinition.getAccessPolicyId());
        assertEquals(contractPolicyId, contractDefinition.getContractPolicyId());
        assertEquals(assetId, ((List<?>) contractDefinition.getAssetsSelector().get(0).getOperandRight()).get(0));
    }

    private void assertAdditionToContractDefinition(String accessPolicyId, String contractPolicyId, String assetId) {
        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertFalse(contractDefinitions.isEmpty());
        ContractDefinition contractDefinition = contractDefinitions.get(contractDefinitions.size() - 1);
        assertEquals(accessPolicyId, contractDefinition.getAccessPolicyId());
        assertEquals(contractPolicyId, contractDefinition.getContractPolicyId());
        List<?> assetSelectorTarget = (List<?>) contractDefinition.getAssetsSelector().get(0).getOperandRight();
        assertEquals(assetId, (assetSelectorTarget).get(assetSelectorTarget.size() - 1));
    }
}
