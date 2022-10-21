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
package de.fraunhofer.iosb.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import de.fraunhofer.iosb.app.testUtils.FileManager;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;

public class EndpointTest {

    private static final String CONFIG_VALUE_PREFIX = "edc.aas.";
    private static ClientAndServer mockServer;

    private String shells = FileManager.loadResource("shells.json");
    private String submodels = FileManager.loadResource("submodels.json");
    private String submodelsNoSubmodelElements = FileManager.loadResource("submodelsNoSubmodelElements.json");
    private String conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");
    private Endpoint endpoint;
    private Map<URL, SelfDescription> selfDescriptionRepo;
    private AssetLoader mockedAssetLoader;
    private ContractDefinitionStore mockedContractDefinitionStore;
    private PolicyDefinitionStore mockedPolicyDefinitionStore;

    @BeforeAll
    public static void initializeLogger() {
        Logger.getInstance().setMonitor(mock(Monitor.class));
    }

    @BeforeEach
    public void setupEndpoint() {
        selfDescriptionRepo = new ConcurrentHashMap<URL, SelfDescription>();
        mockedAssetLoader = mock(AssetLoader.class);
        mockedContractDefinitionStore = mock(ContractDefinitionStore.class);
        mockedPolicyDefinitionStore = mock(PolicyDefinitionStore.class);
        endpoint = new Endpoint(
                selfDescriptionRepo,
                new AasController(
                        new OkHttpClient()),
                new ResourceController(
                        mockedAssetLoader,
                        mockedContractDefinitionStore,
                        mockedPolicyDefinitionStore));
    }

    @AfterEach
    public void shutdownMockServer() {
        if (Objects.nonNull(mockServer) && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @Test
    public void getConfigTest() {
        new ConfigurationController().handleRequest(RequestType.PUT, null, "");

        assertEquals(FileManager.loadResource("defaultConfig.json"),
                endpoint.getConfig().getEntity());
    }

    @Test
    public void changeSingleConfigValueTest() {
        var config = FileManager.loadResource("config.json");
        var configController = new ConfigurationController();
        configController.handleRequest(RequestType.PUT, null, config);

        configController.handleRequest(RequestType.PUT, null,
                "{\"" + CONFIG_VALUE_PREFIX + "exposeselfdescription\":true}");

        assertTrue(Configuration.getInstance().isExposeSelfDescription());
    }

    @Test
    public void putWrongConfigValueTest() {
        var response = endpoint
                .putConfig("{\"" + CONFIG_VALUE_PREFIX + "falseconfigvalue\":12}");

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void postFalseAasServiceTest() throws MalformedURLException {
        endpoint.postAasService(new URL("http://example.com/aas"));

        // No selfDescription has been added
        assertEquals(0, selfDescriptionRepo.values().size());
    }

    @Test
    public void postAasServiceTest() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));
        startMockServer(port);

        prepareEmptyMockedResponse();
        endpoint.postAasService(url);

        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                selfDescriptionRepo.get(url).toString());
    }

    @Test
    public void postAasEnvironmentTest() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));

        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.get(new URL("http://localhost:8080")).toString());

        endpoint.removeAasService(url);

    }

    @Test
    public void removeAasServiceTest() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.get(url).toString());

        endpoint.removeAasService(url);

        assertEquals(null, selfDescriptionRepo.get(url));
    }

    @Test
    public void testSynchronizationRemoveAll() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));
        EndpointTest.startMockServer(port);
        prepareDefaultMockedResponse();

        endpoint.postAasService(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.get(url).toString());

        prepareEmptyMockedResponse();
        endpoint.syncAasWithEdc(url);
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                selfDescriptionRepo.get(url).toString());
    }

    /**
     * Also tests whether a submodel is reloaded into AssetIndex/ContractStore if
     * only its submodelElements were modified
     */
    @Test
    public void testSynchronizationRemoveSubmodelElement() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));
        EndpointTest.startMockServer(port);
        prepareDefaultMockedResponse();

        endpoint.postAasService(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.get(url).toString());

        prepareRemovedSubmodelElementMockedResponse();
        endpoint.syncAasWithEdc(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsNoSubmodelElements.json"),
                selfDescriptionRepo.get(url).toString());
    }

    @Test
    public void testSynchronizationAdd() throws IOException {
        int port = 8080;
        var url = new URL(String.format("http://localhost:%s", port));
        EndpointTest.startMockServer(port);
        prepareEmptyMockedResponse();

        endpoint.postAasService(url);
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                selfDescriptionRepo.get(url).toString());

        prepareDefaultMockedResponse();

        endpoint.syncAasWithEdc(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                selfDescriptionRepo.get(url).toString());
    }

    private void prepareDefaultMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1)).respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1)).respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1)).respond(response().withBody(conceptDescriptions));
    }

    private void prepareEmptyMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1)).respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1)).respond(response().withBody("[]"));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody("[]"));
    }

    private void prepareRemovedSubmodelElementMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1)).respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1)).respond(response().withBody(submodelsNoSubmodelElements));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1)).respond(response().withBody(conceptDescriptions));
    }

    private static void startMockServer(int port) throws IOException {
        mockServer = startClientAndServer(port);
    }

}
