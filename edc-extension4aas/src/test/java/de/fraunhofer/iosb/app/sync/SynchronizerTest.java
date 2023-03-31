package de.fraunhofer.iosb.app.sync;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ResourceController;
import de.fraunhofer.iosb.app.model.ids.SelfDescription;
import de.fraunhofer.iosb.app.testUtils.FileManager;
import okhttp3.OkHttpClient;

public class SynchronizerTest {

    private static int port;
    private static URL url;
    private static ClientAndServer mockServer;

    private Synchronizer synchronizer;
    private Map<URL, SelfDescription> mockedSelfDescriptionRepo;

    private String shells = FileManager.loadResource("shells.json");
    private String submodels = FileManager.loadResource("submodels.json");
    private String submodelsNoSubmodelElements = FileManager.loadResource("submodelsNoSubmodelElements.json");
    private String conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        Logger.getInstance().setMonitor(mock(Monitor.class));
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
    }

    @BeforeEach
    public void setupSynchronizer() {
        mockedSelfDescriptionRepo = new HashMap<URL, SelfDescription>();
        synchronizer = new Synchronizer(
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

    /*
     * Tests initialization of selfDescription
     */
    @Test
    public void synchronizationInitializeTest() throws IOException {
        startMockServer(port);
        prepareDefaultMockedResponse();

        mockedSelfDescriptionRepo.put(url, null);
        assertEquals(null, mockedSelfDescriptionRepo.get(url));

        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());
    }

    @Test
    public void synchronizationRemoveSubmodelElementTest() throws IOException {
        startMockServer(port);

        mockedSelfDescriptionRepo.put(url, null);
        assertEquals(null, mockedSelfDescriptionRepo.get(url));

        prepareDefaultMockedResponse();
        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());

        prepareRemovedSubmodelMockedResponse();
        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIdsNoSubmodelElements.json"),
                mockedSelfDescriptionRepo.get(url).toString());
    }

    @Test
    public void synchronizationRemoveAllTest() throws IOException {
        startMockServer(port);

        mockedSelfDescriptionRepo.put(url, null);
        assertEquals(null, mockedSelfDescriptionRepo.get(url));

        prepareDefaultMockedResponse();
        synchronizer.synchronize();
        assertEquals(FileManager.loadResource("selfDescriptionWithIds.json"),
                mockedSelfDescriptionRepo.get(url).toString());

        prepareEmptyMockedResponse();
        synchronizer.synchronize();
        assertEquals("{\"assetAdministrationShells\":[],\"submodels\":[],\"conceptDescriptions\":[]}",
                mockedSelfDescriptionRepo.get(url).toString());
    }

    private void prepareRemovedSubmodelMockedResponse() {
        mockServer.when(request().withMethod("GET").withPath("/shells"), Times.exactly(1))
                .respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels"), Times.exactly(1))
                .respond(response().withBody(submodelsNoSubmodelElements));
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

    private static void startMockServer(int port) throws IOException {
        mockServer = startClientAndServer(port);
    }

}
