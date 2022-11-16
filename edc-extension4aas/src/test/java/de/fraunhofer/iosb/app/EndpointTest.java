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

import static java.lang.String.format;
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

import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
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
import de.fraunhofer.iosb.app.util.Encoder;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;

/**
 * Not mocking the controllers this endpoint uses, as the mocking/validation
 * process would
 * be pretty complex. Mocking the AAS service instead.
 */
public class EndpointTest {

    private static final String CONFIG_VALUE_PREFIX = "edc.aas.";
    private static ClientAndServer mockServer;

    private String shells = FileManager.loadResource("shells.json");
    private String submodels = FileManager.loadResource("submodels.json");
    private String submodelsNoSubmodelElements = FileManager.loadResource("submodelsNoSubmodelElements.json");
    private String submodelsWithUpdatedSubmodelElement = FileManager
            .loadResource("submodelsWithUpdatedSubmodelElement.json");
    private String conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");
    private Endpoint endpoint;
    private static int port;
    private static URL url;

    private Map<URL, SelfDescription> mockedSelfDescriptionRepo;

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        Logger.getInstance().setMonitor(mock(Monitor.class));
        port = 8080;
        url = new URL(format("http://localhost:%s", port));

    }

    @BeforeEach
    public void setupEndpoint() {
        mockedSelfDescriptionRepo = new ConcurrentHashMap<URL, SelfDescription>();
        endpoint = new Endpoint(
                mockedSelfDescriptionRepo,
                new AasController(
                        new OkHttpClient()),
                new ResourceController(
                        mock(AssetIndex.class),
                        mock(ContractDefinitionStore.class),
                        mock(PolicyDefinitionStore.class)));
    }

    @AfterEach
    public void shutdownMockServer() {
        if (Objects.nonNull(mockServer) && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @Test
    public void getSelfDescriptionWithParamTest() {
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), endpoint.getSelfDescription(url).getStatus());
    }

    @Test
    public void getSelfDescriptionTest() throws IOException {
        startMockServer(port);
        prepareDefaultMockedResponse();
        endpoint.postAasService(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsInList.json"),
                endpoint.getSelfDescription(null).getEntity());
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
        assertEquals(123456, Configuration.getInstance().getSyncPeriod());
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
        assertEquals(0, mockedSelfDescriptionRepo.values().size());
    }

    @Test
    public void postAasServiceTest() throws IOException {
        startMockServer(port);
        prepareEmptyMockedResponse();
        endpoint.postAasService(url);
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                mockedSelfDescriptionRepo.get(url).toString());
    }

    @Test
    public void postAasEnvironmentTest() throws IOException {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(new URL("http://localhost:8080")).toString());

        endpoint.removeAasService(url);
    }

    @Test
    public void removeAasServiceTest() throws IOException {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());

        endpoint.removeAasService(url);

        assertEquals(null, mockedSelfDescriptionRepo.get(url));
    }

    @Test
    public void putAasRequestTest() throws IOException {
        startMockServer(port);
        prepareDefaultMockedResponse();
        endpoint.postAasService(url);
        // prepare updated SubmodelElement mocked response
        mockServer.when(request().withMethod("PUT"), Times.exactly(1)).respond(response().withStatusCode(200));

        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(submodelsWithUpdatedSubmodelElement));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));

        endpoint.putAasRequest(new URL(format(url.toString(), "/submodels/",
                Encoder.encodeBase64("https://example.com/ids/sm/4445_8090_6012_7409"),
                "/submodel-elements/GripperUp")),
                FileManager.loadResource("submodelElement.json"));

        assertEquals(FileManager.loadResource("selfDescriptionWithUpdatedSubmodelElement.json"),
                mockedSelfDescriptionRepo.get(url).toString());
    }

    @Test
    public void synchronizationRemoveAllTest() throws IOException {
        EndpointTest.startMockServer(port);
        prepareDefaultMockedResponse();

        endpoint.postAasService(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());

        prepareEmptyMockedResponse();
        endpoint.syncAasWithEdc(url);
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                mockedSelfDescriptionRepo.get(url).toString());
    }

    /**
     * Also tests whether a submodel/SMC is reloaded into AssetIndex/ContractStore
     * if
     * only its submodelElements were modified (should not happen)
     */
    @Test
    public void synchronizationRemoveSubmodelElementTest() throws IOException {
        EndpointTest.startMockServer(port);
        prepareDefaultMockedResponse();

        endpoint.postAasService(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());

        // prepare "removed SubmodelElement" mocked response
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(submodelsNoSubmodelElements));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"), Times.exactly(1))
                .respond(response().withBody(conceptDescriptions));

        endpoint.syncAasWithEdc(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsNoSubmodelElements.json"),
                mockedSelfDescriptionRepo.get(url).toString());
    }

    @Test
    public void synchronizationAddTest() throws IOException {
        EndpointTest.startMockServer(port);
        prepareEmptyMockedResponse();

        endpoint.postAasService(url);
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                mockedSelfDescriptionRepo.get(url).toString());
        prepareDefaultMockedResponse();

        endpoint.syncAasWithEdc(url);
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());
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

    private static void startMockServer(int port) throws IOException {
        mockServer = startClientAndServer(port);
    }

}
