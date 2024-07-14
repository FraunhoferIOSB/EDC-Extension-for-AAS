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

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.testutils.FileManager;
import de.fraunhofer.iosb.app.testutils.StringMethods;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class SynchronizerTest {

    private static final String PATH_PREFIX = "/api/v3.0";
    private static final String SUBMODELS = PATH_PREFIX + "/submodels/";
    private static final String SHELLS = PATH_PREFIX + "/shells/";
    private static final String CONCEPT_DESCRIPTIONS = PATH_PREFIX + "/concept-descriptions/";
    private static int port;
    private static URL url;
    private static ClientAndServer mockServer;
    private final String shells = FileManager.loadResource("shells.json");
    private final String submodels = FileManager.loadResource("submodels.json");
    private final String submodelsNoSubmodelElements = FileManager.loadResource("submodelsNoSubmodelElements.json");
    private final String oneSubmodelOneSubmodelElementMore = FileManager
            .loadResource("oneSubmodelOneSubmodelElementMore.json");
    private final String conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");
    private Synchronizer synchronizer;
    private SelfDescriptionRepository selfDescriptionRepo;

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
        startMockServer(port);
    }

    @AfterAll
    public static void shutdownMockServer() {
        if (Objects.nonNull(mockServer) && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    private static void startMockServer(int port) {
        mockServer = startClientAndServer(port);
    }

    @BeforeEach
    public void setupSynchronizer() {
        var monitor = new ConsoleMonitor();

        selfDescriptionRepo = new SelfDescriptionRepository();
        synchronizer = Synchronizer.Builder.getInstance()
                .selfDescriptionRepository(selfDescriptionRepo)
                .aasController(new AasController(monitor,
                        new AllAasDataProcessorFactory(
                                new NoOpSelfSignedCertificateRetriever(),
                                new OkHttpClient(),
                                RetryPolicy.ofDefaults(),
                                new ConsoleMonitor())))
                .assetIndex(mock(AssetIndex.class))
                .contractStore(mock(ContractDefinitionStore.class))
                .policyStore(mock(PolicyDefinitionStore.class))
                .monitor(monitor)
                .aasServiceRegistry(new AasServiceRegistry(new HashSet<>()))
                .build();

        selfDescriptionRepo.registerListener(synchronizer);
        prepareDefaultMockedResponse();
    }

    @AfterEach
    public void cleanUp() {
        mockServer.reset();
    }

    /*
     * Tests initialization of selfDescription
     */
    @Test
    public void synchronizationInitializeTest() {
        selfDescriptionRepo.createSelfDescription(url);

        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIds.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAllSubmodelElementsTest() {
        selfDescriptionRepo.createSelfDescription(url);
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIds.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareRemovedSubmodelMockedResponse();
        synchronizer.run();
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIdsNoSubmodelElements.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationAddOneSubmodelElementTest() {
        selfDescriptionRepo.createSelfDescription(url);
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIds.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareAddedSubmodelElementMockedResponse();
        synchronizer.run();
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIdsOneSubmodelOneSubmodelElementMore.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAllTest() {
        selfDescriptionRepo.createSelfDescription(url);
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIds.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());

        prepareEmptyMockedResponse();
        synchronizer.run();
        StringMethods.assertEqualsIgnoreWhiteSpace("{}",
                selfDescriptionRepo.getSelfDescription(url).toString());
    }

    @Test
    public void synchronizationRemoveAasTest() {
        selfDescriptionRepo.createSelfDescription(url);
        StringMethods.assertEqualsIgnoreWhiteSpace(
                Objects.requireNonNull(FileManager.loadResource("selfDescriptionWithIds.json")),
                selfDescriptionRepo.getSelfDescription(url).toString());

        selfDescriptionRepo.removeSelfDescription(url);
        assertNull(selfDescriptionRepo.getSelfDescription(url));
    }

    @Test
    public void aasServiceNotAvailableTest() {
        mockServer.stop();
        try {
            selfDescriptionRepo.createSelfDescription(url);
            fail("AAS service not available, self description should not be created");
        } catch (EdcException expected) {
        }
        startMockServer(port);
    }

    private void prepareRemovedSubmodelMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath(SHELLS), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath(SUBMODELS), Times.exactly(1))
                .respond(response().withBody(submodelsNoSubmodelElements));
        mockServer.when(request().withMethod("GET").withPath(CONCEPT_DESCRIPTIONS), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareAddedSubmodelElementMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath(SHELLS), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath(SUBMODELS), Times.exactly(1))
                .respond(response().withBody(oneSubmodelOneSubmodelElementMore));
        mockServer.when(request().withMethod("GET").withPath(CONCEPT_DESCRIPTIONS), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareDefaultMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath(SHELLS), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath(SUBMODELS), Times.exactly(1))
                .respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath(CONCEPT_DESCRIPTIONS), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));
    }

    private void prepareEmptyMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath(SHELLS), Times.exactly(1))
                .respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath(SUBMODELS), Times.exactly(1))
                .respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath(CONCEPT_DESCRIPTIONS), Times.exactly(1))
                .respond(response().withBody("[]"));
    }

}
