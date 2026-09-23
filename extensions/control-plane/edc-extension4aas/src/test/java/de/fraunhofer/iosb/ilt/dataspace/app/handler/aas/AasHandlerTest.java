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
package de.fraunhofer.iosb.ilt.dataspace.app.handler.aas;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.dataspace.aas.test.defaults.DefaultVault;
import de.fraunhofer.iosb.ilt.dataspace.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.ilt.dataspace.client.repository.AasRepositoryClient;
import de.fraunhofer.iosb.ilt.dataspace.model.context.repository.remote.RemoteAasRepositoryContext;
import de.fraunhofer.iosb.ilt.faaast.client.exception.ConnectivityException;
import de.fraunhofer.iosb.ilt.faaast.client.exception.StatusCodeException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.util.AasUtils;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultEnvironment;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.CriterionOperatorRegistry;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static de.fraunhofer.iosb.ilt.dataspace.app.testutils.AasCreator.getAssetAdministrationShell;
import static de.fraunhofer.iosb.ilt.dataspace.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.ilt.dataspace.app.testutils.AasCreator.getSubmodel;
import static org.eclipse.edc.dataaddress.httpdata.spi.HttpDataAddressSchema.BASE_URL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class AasHandlerTest {

    private final CriterionOperatorRegistry criterionOperatorRegistry = CriterionOperatorRegistryImpl.ofDefaults();
    private final Vault vault = new DefaultVault();
    private final Monitor monitor = new ConsoleMonitor();
    private final Submodel submodel = getSubmodel();
    private final Reference submodelReference = AasUtils.toReference(submodel);
    private final AssetAdministrationShell shell = getAssetAdministrationShell();
    private final Reference shellReference = AasUtils.toReference(shell);

    private EdcStoreHandler edcStoreHandler;
    private InMemoryAssetIndex assetIndex;


    @BeforeEach
    void setUp() {
        assetIndex = new InMemoryAssetIndex(criterionOperatorRegistry);
        InMemoryContractDefinitionStore contractDefinitionStore = new InMemoryContractDefinitionStore(criterionOperatorRegistry);
        edcStoreHandler = new EdcStoreHandler(assetIndex, contractDefinitionStore, () -> "provider");
    }


    private TestAasHandler handlerWith(RemoteAasRepositoryContext context) {
        return new TestAasHandler(monitor, context, vault, edcStoreHandler, getEmptyEnvironment());
    }


    private RemoteAasRepositoryContext contextWith(URI uri, List<PolicyBinding> bindings, boolean onlySubmodels) {
        return new RemoteAasRepositoryContext.Builder()
                .uri(uri)
                .policyBindings(bindings)
                .onlySubmodels(onlySubmodels)
                .build();
    }


    @Test
    void eligibleForRegistration_emptyBindings_allElementsEligible() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        assertTrue(handler.eligibleForRegistration(submodelReference));
        assertTrue(handler.eligibleForRegistration(shellReference));
    }


    @Test
    void eligibleForRegistration_emptyBindingsOnlySubmodels_nonSubmodelNotEligible() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), true));

        assertTrue(handler.eligibleForRegistration(submodelReference));
        assertFalse(handler.eligibleForRegistration(shellReference));
    }


    @Test
    void eligibleForRegistration_withBindings_onlyMatchingEligible() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"),
                List.of(PolicyBinding.ofDefaults(submodelReference)), false));

        assertTrue(handler.eligibleForRegistration(submodelReference));
        assertFalse(handler.eligibleForRegistration(shellReference));
    }


    @Test
    void eligibleForRegistration_withBindingsOnlySubmodels_matchingNonSubmodelNotEligible() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"),
                List.of(PolicyBinding.ofDefaults(shellReference)), true));

        assertFalse(handler.eligibleForRegistration(shellReference));
        assertFalse(handler.eligibleForRegistration(submodelReference));
    }


    // --- getReferences ---


    @Test
    void getReferences_returnsAllBindingReferences() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"),
                List.of(PolicyBinding.ofDefaults(submodelReference),
                        PolicyBinding.ofDefaults(shellReference)),
                false));

        var references = handler.getReferences();
        assertEquals(2, references.size());
        assertTrue(references.contains(submodelReference));
        assertTrue(references.contains(shellReference));
    }


    @Test
    void getReferences_emptyBindings_returnsEmptyList() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        assertTrue(handler.getReferences().isEmpty());
    }


    // --- isOnlySubmodels ---


    @Test
    void isOnlySubmodels_reflectsContextSetting() {
        var handlerWithOnly = handlerWith(contextWith(URI.create("http://test.local"), List.of(), true));
        assertTrue(handlerWithOnly.isOnlySubmodels());

        var handlerWithout = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));
        assertFalse(handlerWithout.isOnlySubmodels());
    }


    // --- policyBindingsFor ---


    @Test
    void policyBindingsFor_default_returnsSingleDefaultBinding() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var bindings = handler.policyBindingsFor(submodelReference);

        assertEquals(1, bindings.size());
        assertEquals(submodelReference, bindings.get(0).referredElement());
    }


    // --- expandBindings ---


    @Test
    void expandBindings_eligibleReference_yieldsOneEntry() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var result = handler.expandBindings(Map.of(submodelReference, createBaseAsset()));

        assertEquals(1, result.size());
        var binding = result.keySet().iterator().next();
        assertEquals(submodelReference, binding.referredElement());
    }


    @Test
    void expandBindings_ineligibleReference_yieldsNoEntries() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), true));

        var result = handler.expandBindings(Map.of(shellReference, createBaseAsset()));

        assertTrue(result.isEmpty());
    }


    @Test
    void expandBindings_multipleEligibleReferences_yieldsOneEntryEach() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var result = handler.expandBindings(Map.of(
                submodelReference, createBaseAsset(),
                shellReference, createBaseAsset()));

        assertEquals(2, result.size());
    }


    @Test
    void expandBindings_nonMatchingBinding_skipsReference() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"),
                List.of(PolicyBinding.ofDefaults(submodelReference)), false));

        var result = handler.expandBindings(Map.of(shellReference, createBaseAsset()));

        assertTrue(result.isEmpty());
    }


    // --- assetForBinding ---


    @Test
    void assetForBinding_distinctBindings_yieldDistinctAssetIds() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var baseAsset = createBaseAsset();
        var binding1 = new PolicyBinding(submodelReference, "access-1", "contract-1");
        var binding2 = new PolicyBinding(submodelReference, "access-2", "contract-2");

        var asset1 = handler.assetForBinding(submodelReference, baseAsset, binding1);
        var asset2 = handler.assetForBinding(submodelReference, baseAsset, binding2);

        assertNotEquals(asset1.getId(), asset2.getId());
        assertEquals(asset1.getId(), handler.assetForBinding(submodelReference, baseAsset, binding1).getId());
    }


    @Test
    void assetForBinding_dataAddressProperties_mergedIntoAsset() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var binding = new PolicyBinding(submodelReference, "access", "contract",
                Map.of(BASE_URL, "http://override.local"));

        var asset = handler.assetForBinding(submodelReference, createBaseAsset(), binding);

        assertEquals("http://override.local", asset.getDataAddress().getStringProperty(BASE_URL));
    }


    @Test
    void assetForBinding_preservesBaseDataAddressType() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var asset = handler.assetForBinding(submodelReference, createBaseAsset(),
                new PolicyBinding(submodelReference, "access", "contract"));

        assertEquals("AasData", asset.getDataAddress().getType());
    }


    @Test
    void assetForBinding_newDataAddressProperty_addedToAsset() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var binding = new PolicyBinding(submodelReference, "access", "contract",
                Map.of("custom-key", "custom-value"));

        var asset = handler.assetForBinding(submodelReference, createBaseAsset(), binding);

        assertEquals("custom-value", asset.getDataAddress().getStringProperty("custom-key"));
        assertEquals("http://test.local", asset.getDataAddress().getStringProperty(BASE_URL));
    }


    @Test
    void assetForBinding_distinctDataAddressProperties_yieldDistinctAssetIds() {
        var handler = handlerWith(contextWith(URI.create("http://test.local"), List.of(), false));

        var baseAsset = createBaseAsset();
        var withoutProps = new PolicyBinding(submodelReference, "access", "contract", Map.of());
        var withProps = new PolicyBinding(submodelReference, "access", "contract",
                Map.of("method", "POST"));

        var asset1 = handler.assetForBinding(submodelReference, baseAsset, withoutProps);
        var asset2 = handler.assetForBinding(submodelReference, baseAsset, withProps);

        assertNotEquals(asset1.getId(), asset2.getId());
    }


    @Test
    void initialize_eligibleElements_registersAssets() throws StatusCodeException, ConnectivityException {
        var context = contextWith(URI.create("http://test.local"), List.of(), false);
        var environment = new DefaultEnvironment.Builder()
                .submodels(List.of(submodel))
                .build();
        var handler = new TestAasHandler(monitor, context, vault, edcStoreHandler, environment);

        var registered = handler.initialize();

        assertFalse(registered.isEmpty());
        assertEquals(registered.size(), assetIndex.countAssets(List.of()));
    }


    @Test
    void initialize_onlySubmodels_shellsNotRegistered() throws StatusCodeException, ConnectivityException {
        var context = contextWith(URI.create("http://test.local"), List.of(), true);
        var environment = new DefaultEnvironment.Builder()
                .assetAdministrationShells(List.of(shell))
                .build();
        var handler = new TestAasHandler(monitor, context, vault, edcStoreHandler, environment);

        var registered = handler.initialize();

        assertTrue(registered.isEmpty());
        assertEquals(0, assetIndex.countAssets(List.of()));
    }


    @Test
    void cleanUp_afterInitialize_clearsAllAssets() throws StatusCodeException, ConnectivityException {
        var context = contextWith(URI.create("http://test.local"), List.of(), false);
        var environment = new DefaultEnvironment.Builder()
                .submodels(List.of(submodel))
                .build();
        var handler = new TestAasHandler(monitor, context, vault, edcStoreHandler, environment);

        handler.initialize();
        assertTrue(assetIndex.countAssets(List.of()) > 0);

        handler.cleanUp();
        assertEquals(0, assetIndex.countAssets(List.of()));
    }


    private static Asset createBaseAsset() {
        return Asset.Builder.newInstance()
                .id("base-id")
                .dataAddress(DataAddress.Builder.newInstance()
                        .type("AasData")
                        .property(BASE_URL, "http://test.local")
                        .build())
                .build();
    }


    private static class TestAasHandler extends AasHandler<AasRepositoryClient, RemoteAasRepositoryContext> {
        private final Environment environment;


        TestAasHandler(Monitor monitor, RemoteAasRepositoryContext context, Vault vault,
                       EdcStoreHandler edcStoreHandler, Environment environment) {
            super(monitor, context, vault, edcStoreHandler);
            this.environment = environment;
        }


        @Override
        protected AasRepositoryClient clientFrom(Vault vault, RemoteAasRepositoryContext context) {
            var mockClient = mock(AasRepositoryClient.class);
            when(mockClient.getUri()).thenReturn(context.getUri());
            return mockClient;
        }


        @Override
        protected Environment getEnvironment() {
            return environment;
        }
    }
}
