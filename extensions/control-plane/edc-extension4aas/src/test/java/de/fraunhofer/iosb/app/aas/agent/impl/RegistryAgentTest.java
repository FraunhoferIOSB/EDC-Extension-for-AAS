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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.app.controller.AasRepositoryController;
import de.fraunhofer.iosb.app.controller.dto.AasRegistryContextDTO;
import de.fraunhofer.iosb.app.handler.aas.registry.RemoteAasRegistryHandler;
import de.fraunhofer.iosb.app.handler.edc.EdcStoreHandler;
import de.fraunhofer.iosb.app.stores.repository.AasServerStore;
import de.fraunhofer.iosb.client.exception.UnauthorizedException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException;
import de.fraunhofer.iosb.ilt.faaast.service.dataformat.json.JsonApiSerializer;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.Page;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.paging.PagingMetadata;
import de.fraunhofer.iosb.ilt.faaast.service.model.exception.UnsupportedModifierException;
import de.fraunhofer.iosb.repository.impl.faaast.FaaastRepositoryManager;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Extension;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.assetindex.InMemoryAssetIndex;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static de.fraunhofer.iosb.aas.lib.model.impl.Registry.SHELL_DESCRIPTORS_PATH;
import static de.fraunhofer.iosb.aas.lib.model.impl.Registry.SUBMODEL_DESCRIPTORS_PATH;
import static de.fraunhofer.iosb.api.model.HttpMethod.GET;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asShell;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.asSubmodel;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getShellDescriptor;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

class RegistryAgentTest {
    private static final int PORT = getFreePort();
    private static ClientAndServer mockServer;
    private AasRepositoryController testSubject;
    private AasServerStore repository;
    private AssetIndex assetIndex;
    private ContractDefinitionStore contractDefinitionStore;
    private final JsonApiSerializer jsonApiSerializer = new JsonApiSerializer();
    private final URI mockServerUri = new URI("http://localhost:%s".formatted(PORT));

    RegistryAgentTest() throws URISyntaxException {
    }

    @BeforeAll
    static void initialize() {
        mockServer = startClientAndServer(PORT);
    }

    @BeforeEach
    void setUp() {
        repository = new AasServerStore();
        var criterionRegistry = CriterionOperatorRegistryImpl.ofDefaults();
        assetIndex = new InMemoryAssetIndex(criterionRegistry);
        contractDefinitionStore = new InMemoryContractDefinitionStore(criterionRegistry);

        var monitor = new ConsoleMonitor().withPrefix(RegistryAgentTest.class.getSimpleName());

        testSubject = new AasRepositoryController(monitor, repository, new FaaastRepositoryManager(monitor, () -> "localhost"),
                new EdcStoreHandler(assetIndex, contractDefinitionStore));
    }

    @AfterAll
    static void shutdown() {
        mockServer.stop();
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
    }

    @Test
    void test_register_singleShellDescriptorIncludingASubmodelDescriptor() throws UnauthorizedException, ConnectException
            , UnsupportedModifierException, de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException {
        AssetAdministrationShellDescriptor shellDescriptor = getShellDescriptor();

        Page<AssetAdministrationShellDescriptor> descriptorPage =
                Page.<AssetAdministrationShellDescriptor>builder()
                        .result(shellDescriptor)
                        .metadata(PagingMetadata.builder().build())
                        .build();

        var mockedResponseBody = jsonApiSerializer.write(descriptorPage);

        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SHELL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(mockedResponseBody));

        mockEmptySubmodelRequest();

        var result = testSubject.register(new AasRegistryContextDTO(mockServerUri, new NoAuth()));

        assertEquals(result, mockServerUri);

        var handler = (RemoteAasRegistryHandler) repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertFalse(selfDescription.getAssetAdministrationShells().isEmpty());
        assertFalse(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());

        var shell = selfDescription.getAssetAdministrationShells().get(0);
        var submodel = selfDescription.getSubmodels().get(0);

        // Remove assetId
        Extension shellAssetIdExtension = shell.getExtensions().remove(0);
        Extension submodelAssetIdExtension = submodel.getExtensions().remove(0);

        var expectedAsSubmodel = asSubmodel(shellDescriptor.getSubmodelDescriptors().get(0));
        var expectedAsShell = asShell(shellDescriptor);

        assertEquals(expectedAsShell, shell);
        assertEquals(expectedAsSubmodel, submodel);

        var assets = assetIndex.queryAssets(QuerySpec.max()).toList();

        assertEquals(2, assets.size());
        assertTrue(assets.stream().map(Asset::getId).toList().contains(submodelAssetIdExtension.getValue()));
        assertTrue(assets.stream().map(Asset::getId).toList().contains(shellAssetIdExtension.getValue()));

        var contractDefinitions = contractDefinitionStore.findAll(QuerySpec.max()).toList();

        assertEquals(1, contractDefinitions.size());
        var contractDefinitionAssetIds = (List<String>) contractDefinitions.get(0).getAssetsSelector().get(0).getOperandRight();
        assertTrue(contractDefinitionAssetIds.contains(submodelAssetIdExtension.getValue()));
        assertTrue(contractDefinitionAssetIds.contains(shellAssetIdExtension.getValue()));
    }

    @Test
    void test_register_emptyDescriptorResponseNoFault() throws SerializationException, UnsupportedModifierException, UnauthorizedException,
            ConnectException {
        mockEmptyShellRequest();
        mockEmptySubmodelRequest();

        var result = testSubject.register(new AasRegistryContextDTO(mockServerUri, new NoAuth()));

        assertEquals(result, mockServerUri);

        var handler = (RemoteAasRegistryHandler) repository.get(result).orElseThrow();

        var selfDescription = handler.buildSelfDescription();

        assertSelfDescription(selfDescription);
        assertTrue(selfDescription.getAssetAdministrationShells().isEmpty());
        assertTrue(selfDescription.getSubmodels().isEmpty());
        assertTrue(selfDescription.getConceptDescriptions().isEmpty());
    }

    @Test
    void test_register_notARegistryFailsRegistration() throws UnauthorizedException {
        URI uri;
        try {
            uri = new URI("https://example.com");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        try {
            testSubject.register(new AasRegistryContextDTO(uri, new NoAuth()));
            fail();
        } catch (ConnectException ignored) {
        }
    }

    private static void assertSelfDescription(Environment selfDescription) {
        assertNotNull(selfDescription);
        assertNotNull(selfDescription.getAssetAdministrationShells());
        assertNotNull(selfDescription.getSubmodels());
        assertNotNull(selfDescription.getConceptDescriptions());
    }

    private void mockEmptyShellRequest() throws SerializationException, UnsupportedModifierException {
        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SHELL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(jsonApiSerializer.write(Page.builder()
                                .metadata(PagingMetadata.builder().build())
                                .build())));
    }

    private void mockEmptySubmodelRequest() throws de.fraunhofer.iosb.ilt.faaast.service.dataformat.SerializationException,
            UnsupportedModifierException {
        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SUBMODEL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(jsonApiSerializer.write(Page.builder()
                                .metadata(PagingMetadata.builder().build())
                                .build())));
    }
}