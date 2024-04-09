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
package de.fraunhofer.iosb.app.sync;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.testutils.FileManager;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class SynchronizerTest {

    private static int port;
    private static URL url;
    private static ClientAndServer mockServer;

    private Synchronizer synchronizer;
    private SelfDescriptionRepository selfDescriptionRepo;

    private final String shells = FileManager.loadResource("shells.json");
    private final String submodels = FileManager.loadResource("submodels.json");
    private final String submodelsNoSubmodelElements = FileManager.loadResource("submodelsNoSubmodelElements.json");
    private final String oneSubmodelOneSubmodelElementLess = FileManager
            .loadResource("oneSubmodelOneSubmodelElementLess.json");
    private final String conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
    }

    @BeforeEach
    public void setupSynchronizer() {
        selfDescriptionRepo = new SelfDescriptionRepository();
        synchronizer = new Synchronizer(
                selfDescriptionRepo,
                new AasController(
                        new OkHttpClient()),
                new ResourceController(
                        mock(AssetIndex.class),
                        mock(ContractDefinitionStore.class),
                        mock(PolicyDefinitionStore.class)));
        selfDescriptionRepo.registerListener(synchronizer);
    }

    @AfterEach
    public void shutdownMockServer() {
        if (Objects.nonNull(mockServer) && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    /*
     * Tests initialization of selfDescription
     */
    @Test
    public void synchronizationInitializeTest() {
        startMockServer(port);
        prepareDefaultMockedResponse();

        selfDescriptionRepo.createSelfDescription(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAllSubmodelElementsTest() {
        startMockServer(port);

        prepareDefaultMockedResponse();
        selfDescriptionRepo.createSelfDescription(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareRemovedSubmodelMockedResponse();
        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsNoSubmodelElements.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveOneSubmodelElementTest() {
        startMockServer(port);

        prepareDefaultMockedResponse();
        selfDescriptionRepo.createSelfDescription(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareRemovedSubmodelElementMockedResponse();
        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsOneSubmodelOneSubmodelElementLess.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAllTest() {
        startMockServer(port);

        prepareDefaultMockedResponse();
        selfDescriptionRepo.createSelfDescription(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareEmptyMockedResponse();
        synchronizer.synchronize();
        assertEquals("{}",
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAasTest() {
        startMockServer(port);

        prepareDefaultMockedResponse();
        selfDescriptionRepo.createSelfDescription(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.getSelfDescription(url).toString());

        selfDescriptionRepo.removeSelfDescription(url);
        assertNull(selfDescriptionRepo.getSelfDescription(url));
    }

    @Test
    public void aasServiceNotAvailableTest() {
        try {
            selfDescriptionRepo.createSelfDescription(url);
            fail("AAS service not available, self description should not be created");
        } catch (EdcException expected) {
        }
    }

    private void prepareRemovedSubmodelMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(submodelsNoSubmodelElements));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareRemovedSubmodelElementMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(oneSubmodelOneSubmodelElementLess));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareDefaultMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareEmptyMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody("[]"));
    }

    private static void startMockServer(int port) {
        mockServer = startClientAndServer(port);
    }

}
