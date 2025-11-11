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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.app.controller.dto.AasRegistryContextDTO;
import de.fraunhofer.iosb.app.controller.dto.RemoteAasRepositoryContextDTO;
import de.fraunhofer.iosb.app.handler.aas.registry.RemoteAasRegistryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.app.testutils.MockServerTestExtension;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.Page;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingMetadata;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Extension;
import org.eclipse.digitaltwin.aas4j.v3.model.Identifiable;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.query.QuerySpec;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static de.fraunhofer.iosb.aas.lib.model.impl.Registry.SHELL_DESCRIPTORS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Registry.SUBMODEL_DESCRIPTORS_PATH;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEmptyEnvironment;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getEnvironment;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asShell;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asSubmodel;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getShellDescriptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class AasServerRepositoryIT extends MockServerTestExtension {
    private AasRepositoryController testSubject;
    private AasServerStore repository;
    private AssetIndex assetIndex;
    private ContractDefinitionStore contractDefinitionStore;

    @BeforeEach
    void setUp() {
        repository = new AasServerStore();
        var criterionRegistry = CriterionOperatorRegistryImpl.ofDefaults();
        assetIndex = new InMemoryAssetIndex(criterionRegistry);
        contractDefinitionStore = new InMemoryContractDefinitionStore(criterionRegistry);

        testSubject = new AasRepositoryController(monitor, repository, new FaaastRepositoryManager(monitor, () -> "localhost"),
                new EdcStoreHandler(assetIndex, contractDefinitionStore));
    }

    @Test
    void test_register_singleShellDescriptorIncludingASubmodelDescriptor() throws UnauthorizedException, ConnectException
            , UnsupportedModifierException, de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException {
        AssetAdministrationShellDescriptor shellDescriptor = getShellDescriptor();

        mockResponse(METHOD.GET, String.format("/%s", SHELL_DESCRIPTORS_PATH), asPage(List.of(shellDescriptor)), 200);
        mockEmptySubmodelDescriptorRequest();

        var result = testSubject.register(new AasRegistryContextDTO(getUri(), new NoAuth()));

        assertEquals(result, getUri());

        var handler = (RemoteAasRegistryHandler) repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertFalse(selfDescription.getAssetAdministrationShells().isEmpty());
        assertFalse(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());

        assertIdentifiables(List.of(asShell(shellDescriptor)), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(List.of(asSubmodel(shellDescriptor.getSubmodelDescriptors().get(0))), selfDescription.getSubmodels());
    }

    private static @NotNull URI getUri() {
        return URI.create(server.baseUrl());
    }

    @Test
    void test_register_emptyDescriptorResponseNoFault() throws SerializationException, UnsupportedModifierException, UnauthorizedException,
            ConnectException {
        mockEmptyShellDescriptorRequest();
        mockEmptySubmodelDescriptorRequest();

        var result = testSubject.register(new AasRegistryContextDTO(getUri(), new NoAuth()));

        assertEquals(result, getUri());

        var handler = (RemoteAasRegistryHandler) repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertTrue(selfDescription.getAssetAdministrationShells().isEmpty());
        assertTrue(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());
    }

    @Test
    void test_register_notARegistryFailsRegistration() {
        URI uri;
        try {
            uri = new URI("https://example.com");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            testSubject.register(new AasRegistryContextDTO(uri, new NoAuth()));
            fail();
        } catch (WebApplicationException expected) {
        }
    }

    @Test
    void test_registerRepository_emptyEnvironmentNoFailure() throws SerializationException, UnsupportedModifierException, UnauthorizedException,
            ConnectException {
        mockEmptyShellRequest();
        mockEmptySubmodelRequest();
        mockEmptyConceptDescriptionRequest();

        var result = testSubject.register(new RemoteAasRepositoryContextDTO(getUri(), new NoAuth(), List.of()));

        assertEquals(result, getUri());

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertTrue(selfDescription.getAssetAdministrationShells().isEmpty());
        assertTrue(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());
    }

    @Test
    void test_registerRepository_filledEnvironmentAllRegistered() throws UnauthorizedException,
            ConnectException, SerializationException, UnsupportedModifierException {
        Environment environment = getEnvironment();

        mockResponse(METHOD.GET, "/shells", asPage(environment.getAssetAdministrationShells()), 200);
        mockResponse(METHOD.GET, "/submodels", asPage(environment.getSubmodels()), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", asPage(environment.getConceptDescriptions()), 200);

        var result = testSubject.register(new RemoteAasRepositoryContextDTO(URI.create(server.baseUrl()), new NoAuth(), List.of()));

        assertEquals(result, getUri());

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);

        assertIdentifiables(environment.getAssetAdministrationShells(), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(environment.getSubmodels().stream().peek(sm -> sm.setSubmodelElements(List.of())).toList(),
                selfDescription.getSubmodels());
        assertIdentifiables(environment.getConceptDescriptions(), selfDescription.getConceptDescriptions());
    }

    @Test
    void test_registerRepository_emtpyEnvironmentNoFault() throws UnauthorizedException,
            ConnectException, SerializationException, UnsupportedModifierException {
        Environment environment = getEmptyEnvironment();

        mockResponse(METHOD.GET, "/shells", emptyPage(), 200);
        mockResponse(METHOD.GET, "/submodels", emptyPage(), 200);
        mockResponse(METHOD.GET, "/concept-descriptions", emptyPage(), 200);

        var result = testSubject.register(new RemoteAasRepositoryContextDTO(URI.create(server.baseUrl()), new NoAuth(), List.of()));

        assertEquals(result, getUri());

        var handler = repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);

        assertIdentifiables(environment.getAssetAdministrationShells(), selfDescription.getAssetAdministrationShells());
        assertIdentifiables(environment.getSubmodels().stream().peek(sm -> sm.setSubmodelElements(List.of())).toList(),
                selfDescription.getSubmodels());
        assertIdentifiables(environment.getConceptDescriptions(), selfDescription.getConceptDescriptions());
    }

    @Test
    void test_registerRepository_emtpyEnvironment_shouldThrow() {
        try {
            testSubject.register(new RemoteAasRepositoryContextDTO(URI.create("https://locaIhost:65432/"), new NoAuth(),
                    List.of()));
            fail();
        } catch (WebApplicationException expected) {
        }
    }

    private <T extends Identifiable> void assertIdentifiables(List<T> identifiablesShould, List<T> identifiablesIs) {
        assertEquals(identifiablesIs.size(), identifiablesShould.size());

        List<Extension> extensionsIs = new ArrayList<>();
        for (T aas : identifiablesIs) {
            // In this test, they don't have extensions beforehand.
            extensionsIs.add(aas.getExtensions().remove(0));
            assertTrue(identifiablesShould.contains(aas));
        }

        assertEquals(identifiablesShould.size(), extensionsIs.size());
        List<String> assetIds = extensionsIs.stream().map(Extension::getValue).toList();
        assetIds.forEach(this::assertStores);
    }

    private void assertStores(String assetId) {
        var assets = assetIndex.queryAssets(QuerySpec.max()).toList();

        assets.stream().filter(a -> assetId.equals(a.getId())).findAny().orElseThrow();

        var contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();

        assertEquals(1, contractDefinitions.size());
        var contractDefinitionAssetIds = (List<String>) contractDefinitions.get(0).getAssetsSelector().get(0).getOperandRight();
        assertTrue(contractDefinitionAssetIds.contains(assetId));
    }

    private static void assertSelfDescription(Environment selfDescription) {
        assertNotNull(selfDescription);
        assertNotNull(selfDescription.getAssetAdministrationShells());
        assertNotNull(selfDescription.getSubmodels());
        assertNotNull(selfDescription.getConceptDescriptions());
    }

    private void mockEmptyShellDescriptorRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, String.format("/%s", SHELL_DESCRIPTORS_PATH), emptyPage(), 200);
    }

    private void mockEmptyShellRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/shells", emptyPage(), 200);
    }

    private void mockEmptySubmodelRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/submodels", emptyPage(), 200);
    }

    private void mockEmptyConceptDescriptionRequest() throws SerializationException, UnsupportedModifierException {
        mockResponse(METHOD.GET, "/concept-descriptions", emptyPage(), 200);
    }

    private void mockEmptySubmodelDescriptorRequest() throws de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException,
            UnsupportedModifierException {
        mockResponse(METHOD.GET, String.format("/%s", SUBMODEL_DESCRIPTORS_PATH), emptyPage(), 200);
    }

    private Page<?> emptyPage() {
        return Page.builder()
                .result(List.of())
                .metadata(PagingMetadata.builder().build())
                .build();
    }

    private <T> Page<T> asPage(List<T> ts) {
        return Page.<T>builder()
                .result(ts)
                .metadata(PagingMetadata.builder().build())
                .build();
    }

}