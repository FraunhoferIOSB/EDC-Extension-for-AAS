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
package de.fraunhofer.iosb.app.handler.edc;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.aas.test.defaults.DefaultVault;
import de.fraunhofer.iosb.app.aas.mapper.referable.identifiable.IdentifiableMapper;
import de.fraunhofer.iosb.app.aas.mapper.util.AssetIdUtil;
import de.fraunhofer.iosb.app.handler.util.DataAddressMerger;
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
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getSubmodel;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V31_NAMESPACE;
import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_METHOD;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class EdcStoreHandlerTest {

    private final CriterionOperatorRegistry criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();
    private final IdentifiableMapper identifiableMapper = new IdentifiableMapper(new RemoteAasRepositoryClient(new DefaultVault(), new RemoteAasRepositoryContext.Builder()
            .uri(URI.create("http://invalid.local"))
            .build()));
    private EdcStoreHandler testSubject;
    private InMemoryAssetIndex assetIndex;
    private InMemoryContractDefinitionStore contractDefinitionStore;


    private static void assertEqualAssets(Asset asset, Asset otherAsset) {
        // ParticipantContextId is not namespaced, i.e. cannot be verified
        assertNotNull(otherAsset);
        assertEquals(asset.getId(), otherAsset.getId());
        assertEquals(asset.getDescription(), otherAsset.getDescription());
        assertEquals(asset.getProperties(), otherAsset.getProperties());
    }


    @BeforeEach
    void setUp() {
        assetIndex = new InMemoryAssetIndex(criterionOperatorRegistry);
        contractDefinitionStore = new InMemoryContractDefinitionStore(criterionOperatorRegistry);
        testSubject = new EdcStoreHandler(assetIndex, contractDefinitionStore, () -> "provider");
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
        assertEqualAssets(anotherAsset, assetIndex.findById(anotherAsset.getId()));

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
                .property(AAS_V31_NAMESPACE.concat("modelingType"), "NewProperty")
                .build();

        StoreResult<Asset> result = testSubject.update(assetUpdated);
        assertTrue(result.succeeded());

        Asset storedAsset = assetIndex.findById(asset.getId());
        assertEqualAssets(assetUpdated, storedAsset);

        assertSingleContractDefinition(
                policyBinding.accessPolicyDefinitionId(),
                policyBinding.contractPolicyDefinitionId(),
                asset.getId());
    }


    @Test
    void update_inexistentAsset_shouldNotAddIt() {
        Submodel submodel = getSubmodel();
        Asset asset = identifiableMapper.map(submodel);

        // Intentionally commented out. Do not register this asset.
        // assertRegister(policyBinding, asset);
        Asset assetUpdated = asset.toBuilder()
                .property(AAS_V31_NAMESPACE.concat("modelingType"), "NewProperty")
                .build();

        StoreResult<Asset> result = testSubject.update(assetUpdated);
        assertTrue(result.failed());

        Asset assetIs = assetIndex.findById(asset.getId());
        assertNull(assetIs);

        List<ContractDefinition> contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();
        assertTrue(contractDefinitions.isEmpty());
    }


    @Test
    void assetIdUtil_distinctBindings_shouldYieldDistinctIds() {
        Reference reference = AasUtils.toReference(getSubmodel());
        String url = "http://invalid.local";

        PolicyBinding firstBinding = new PolicyBinding(reference, "access-policy-1", "contract-policy-1");
        PolicyBinding secondBinding = new PolicyBinding(reference, "access-policy-2", "contract-policy-2");

        String firstId = AssetIdUtil.id(url, reference, firstBinding);
        String secondId = AssetIdUtil.id(url, reference, secondBinding);

        assertNotEquals(firstId, secondId);
        assertEquals(firstId, AssetIdUtil.id(url, reference, firstBinding));
    }


    @Test
    void assetIdUtil_distinctDataAddressProperties_shouldYieldDistinctIds() {
        Reference reference = AasUtils.toReference(getSubmodel());
        String url = "http://invalid.local";

        PolicyBinding withoutProperties = new PolicyBinding(reference, "access-policy", "contract-policy", Map.of());
        PolicyBinding withProperties = new PolicyBinding(reference, "access-policy", "contract-policy", Map.of("method", "POST"));

        assertNotEquals(
                AssetIdUtil.id(url, reference, withoutProperties),
                AssetIdUtil.id(url, reference, withProperties));
    }


    @Test
    void dataAddressMerger_bindingProperties_shouldOverrideBase() {
        DataAddress base = DataAddress.Builder.newInstance()
                .type("AasData")
                .property(BASE_URL, "http://base.invalid.local")
                .property("custom", "base-value")
                .build();

        DataAddress merged = DataAddressMerger.merge(base, Map.of(BASE_URL, "http://override.invalid.local", "new-key", "new-value"));

        assertEquals("AasData", merged.getType());
        assertEquals("http://override.invalid.local", merged.getStringProperty(BASE_URL));
        assertEquals("base-value", merged.getStringProperty("custom"));
        assertEquals("new-value", merged.getStringProperty("new-key"));
    }


    @Test
    void register_multipleBindingsSameElement_shouldRegisterDistinctAssets() {
        Submodel submodel = getSubmodel();
        Asset baseAsset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        String url = "http://invalid.local";

        PolicyBinding firstBinding = new PolicyBinding(reference, "access-policy-1", "contract-policy-1");
        PolicyBinding secondBinding = new PolicyBinding(reference, "access-policy-2", "contract-policy-2");

        Asset firstAsset = assetForBinding(url, reference, baseAsset, firstBinding);
        Asset secondAsset = assetForBinding(url, reference, baseAsset, secondBinding);

        assertNotEquals(firstAsset.getId(), secondAsset.getId());

        assertTrue(testSubject.register(firstBinding, firstAsset).succeeded());
        assertTrue(testSubject.register(secondBinding, secondAsset).succeeded());

        assertEquals(2, assetIndex.countAssets(List.of()));
        assertNotNull(assetIndex.findById(firstAsset.getId()));
        assertNotNull(assetIndex.findById(secondAsset.getId()));

        assertEquals(2, contractDefinitionStore.findAll(QuerySpec.max()).count());
        assertContractDefinitionFor(firstBinding.accessPolicyDefinitionId(), firstBinding.contractPolicyDefinitionId(), firstAsset.getId());
        assertContractDefinitionFor(secondBinding.accessPolicyDefinitionId(), secondBinding.contractPolicyDefinitionId(), secondAsset.getId());
    }


    @Test
    void register_bindingWithDataAddressProperties_shouldStoreMergedDataAddress() {
        Submodel submodel = getSubmodel();
        Asset baseAsset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        String url = "http://invalid.local";

        PolicyBinding binding = new PolicyBinding(reference, "access-policy", "contract-policy",
                Map.of(PROXY_METHOD, "POST"));

        Asset asset = assetForBinding(url, reference, baseAsset, binding);

        assertTrue(testSubject.register(binding, asset).succeeded());

        Asset storedAsset = assetIndex.findById(asset.getId());
        assertNotNull(storedAsset);
        assertEquals("http://override.invalid.local", storedAsset.getDataAddress().getStringProperty(BASE_URL));
    }


    @Test
    void register_bindingWithDataAddressProperties_shouldAddCustomDataAddressProperties() {
        Submodel submodel = getSubmodel();
        Asset baseAsset = identifiableMapper.map(submodel);
        Reference reference = AasUtils.toReference(submodel);
        String url = "http://invalid.local";

        PolicyBinding binding = new PolicyBinding(reference, "access-policy", "contract-policy",
                Map.of(PROXY_METHOD, "POST", "AnyCustomProp", "MyValue"));

        Asset asset = assetForBinding(url, reference, baseAsset, binding);

        assertTrue(testSubject.register(binding, asset).succeeded());

        Asset storedAsset = assetIndex.findById(asset.getId());
        assertNotNull(storedAsset);
        assertEquals("POST", storedAsset.getDataAddress().getStringProperty(PROXY_METHOD));
        assertEquals("MyValue", storedAsset.getDataAddress().getStringProperty("AnyCustomProp"));
    }


    private static Asset assetForBinding(String url, Reference reference, Asset baseAsset, PolicyBinding binding) {
        var mergedDataAddress = DataAddressMerger.merge(baseAsset.getDataAddress(), binding.dataAddressProperties());
        return baseAsset.toBuilder()
                .id(AssetIdUtil.id(url, reference, binding))
                .dataAddress(mergedDataAddress)
                .build();
    }


    private void assertRegister(PolicyBinding policyBinding, Asset asset) {
        StoreResult<Void> result = testSubject.register(policyBinding, asset);

        assertTrue(result.succeeded());

        assertEquals(1, assetIndex.countAssets(List.of()));
        Asset storedAsset = assetIndex.findById(asset.getId());
        assertEqualAssets(asset, storedAsset);

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


    private void assertContractDefinitionFor(String accessPolicyId, String contractPolicyId, String assetId) {
        ContractDefinition contractDefinition = contractDefinitionStore.findAll(QuerySpec.max())
                .filter(cd -> cd.getAccessPolicyId().equals(accessPolicyId) && cd.getContractPolicyId().equals(contractPolicyId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No contract definition for " + accessPolicyId + "/" + contractPolicyId));
        List<?> assetSelectorTarget = (List<?>) contractDefinition.getAssetsSelector().get(0).getOperandRight();
        assertTrue(assetSelectorTarget.contains(assetId));
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
