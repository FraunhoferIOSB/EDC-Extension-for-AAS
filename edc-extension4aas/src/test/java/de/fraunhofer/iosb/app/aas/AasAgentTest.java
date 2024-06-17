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
package de.fraunhofer.iosb.app.aas;

import de.fraunhofer.iosb.app.testutils.FileManager;
import de.fraunhofer.iosb.app.util.AssetAdministrationShellUtil;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Testing AAS Agent. Using mocked AAS service (HTTP endpoints)
 */
public class AasAgentTest {

    private static final int PORT = 42042;
    private static final String HTTP_LOCALHOST_8080 = "http://localhost:" + PORT;
    private static final String PATH_PREFIX = "/api/v3.0";
    private static final String SUBMODELS = PATH_PREFIX + "/submodels";
    private static final String SHELLS = PATH_PREFIX + "/shells";
    private static final String CONCEPT_DESCRIPTIONS = PATH_PREFIX + "/concept-descriptions";

    private AasAgent aasAgent;
    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(PORT);
    }

    @BeforeEach
    public void initialize() {
        aasAgent = new AasAgent(new ConsoleMonitor());
        mockServer.reset();
    }

    @Test
    public void testGetAasEnvWithUrls() throws IOException {
        prepareServerResponse();

        var result = aasAgent.getAasEnvWithUrls(new URL(HTTP_LOCALHOST_8080), false);

        // Test if URLs are valid
        AssetAdministrationShellUtil.getAllElements(result).forEach(elem -> assertEquals(HTTP_LOCALHOST_8080 + PATH_PREFIX, elem.getSourceUrl()));
    }

    @Test
    public void testGetAasEnvWithUrlsOnlySubmodels() throws IOException {
        prepareServerResponse();

        var result = aasAgent.getAasEnvWithUrls(new URL(HTTP_LOCALHOST_8080), true);

        // Test if URLs are valid
        AssetAdministrationShellUtil.getAllElements(result).forEach(elem -> assertEquals(HTTP_LOCALHOST_8080 + PATH_PREFIX, elem.getSourceUrl()));

        assertEquals(AssetAdministrationShellUtil.getAllElements(result).stream().filter(elem -> elem instanceof SubmodelElement).count(), 0);
    }

    @Test
    public void testPutAasShell() throws MalformedURLException {
        mockServer.when(request().withMethod("PUT").withPath(SHELLS).withBody("raw_data_forwarded"))
                .respond(response().withStatusCode(200));

        var response = aasAgent.putModel(new URL(HTTP_LOCALHOST_8080 + SHELLS),
                "raw_data_forwarded");

        // Check whether AAS agent forwards the raw data of a request
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPostAasSubmodel() throws MalformedURLException {
        mockServer.when(request().withMethod("POST").withPath(SUBMODELS).withBody("raw_data_forwarded"))
                .respond(response().withStatusCode(200));

        try (var response = aasAgent.postModel(new URL(HTTP_LOCALHOST_8080 + SUBMODELS),
                "raw_data_forwarded")) {
            // Check whether AAS agent forwards the raw data of a request
            assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void testDeleteAasConceptDescription() throws MalformedURLException {
        mockServer.when(request().withMethod("DELETE").withPath(CONCEPT_DESCRIPTIONS))
                .respond(response().withStatusCode(200));

        try (var response = aasAgent.deleteModel(new URL(HTTP_LOCALHOST_8080 + CONCEPT_DESCRIPTIONS), null)) {
            // Check whether AAS agent forwards the raw data of a request
            assertEquals(200, response.getStatus());
        }
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }


    private void prepareServerResponse() {
        var shells = FileManager.loadResource("shells.json");
        var submodels = FileManager.loadResource("submodels.json");
        var conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");

        mockServer.when(request().withMethod("GET").withPath(SHELLS)).respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath(SUBMODELS)).respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath(CONCEPT_DESCRIPTIONS))
                .respond(response().withBody(conceptDescriptions));
    }
}
