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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.fraunhofer.iosb.app.testUtils.FileManager;
import io.adminshell.aas.v3.dataformat.DeserializationException;
import okhttp3.OkHttpClient;

/**
 * Testing AAS Agent. Using mocked AAS service (HTTP endpoints)
 */
public class AasAgentTest {

    /**
     *
     */
    private static final String HTTP_LOCALHOST_8080 = "http://localhost:8080";

    private AasAgent aasAgent;
    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(8080);
    }

    @BeforeEach
    public void initializeAasAgent() {
        aasAgent = new AasAgent(new OkHttpClient());
    }

    @Test
    public void testGetAasEnvWithUrls() throws IOException, DeserializationException {
        var shells = FileManager.loadResource("shells.json");
        var submodels = FileManager.loadResource("submodels.json");
        var conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");

        mockServer.when(request().withMethod("GET").withPath("/shells")).respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels")).respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"))
                .respond(response().withBody(conceptDescriptions));

        var shouldBeResult = FileManager.loadResource("selfDescriptionWithAccessURLS.json");

        var result = new ObjectMapper().writeValueAsString(
                aasAgent.getAasEnvWithUrls(new URL(HTTP_LOCALHOST_8080), false));
        result = result.replace("\n", "").replace(" ", "");

        assertEquals(shouldBeResult, result);
    }

    @Test
    public void testGetAasEnvWithUrlsOnlySubmodels() throws IOException, DeserializationException {
        var shells = FileManager.loadResource("shells.json");
        var submodels = FileManager.loadResource("submodels.json");
        var conceptDescriptions = FileManager.loadResource("conceptDescriptions.json");

        mockServer.when(request().withMethod("GET").withPath("/shells")).respond(response().withBody(shells));
        mockServer.when(request().withMethod("GET").withPath("/submodels")).respond(response().withBody(submodels));
        mockServer.when(request().withMethod("GET").withPath("/concept-descriptions"))
                .respond(response().withBody(conceptDescriptions));

        var shouldBeResult = FileManager.loadResource("selfDescriptionWithAccessURLsSubmodelsOnly.json");

        var result = new ObjectMapper().writeValueAsString(
                aasAgent.getAasEnvWithUrls(new URL(HTTP_LOCALHOST_8080), true));
        result = result.replace("\n", "").replace(" ", "");

        assertEquals(shouldBeResult, result);
    }

    @Test
    public void testPutAasShell() throws MalformedURLException {
        mockServer.when(request().withMethod("PUT").withPath("/shells").withBody("raw_data_forwarded"))
                .respond(response().withStatusCode(200));

        var response = aasAgent.putModel(new URL(HTTP_LOCALHOST_8080 + "/shells"),
                "raw_data_forwarded");

        // Check whether AAS agent forwards the raw data of a request
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPostAasSubmodel() throws MalformedURLException {
        mockServer.when(request().withMethod("POST").withPath("/submodels").withBody("raw_data_forwarded"))
                .respond(response().withStatusCode(200));

        var response = aasAgent.postModel(new URL(HTTP_LOCALHOST_8080 + "/submodels"),
                "raw_data_forwarded");

        // Check whether AAS agent forwards the raw data of a request
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteAasConceptDescription() throws MalformedURLException {
        mockServer.when(request().withMethod("DELETE").withPath("/concept-descriptions"))
                .respond(response().withStatusCode(200));

        var response = aasAgent.deleteModel(new URL(HTTP_LOCALHOST_8080 + "/concept-descriptions"), null);

        // Check whether AAS agent forwards the raw data of a request
        assertEquals(200, response.getStatus());
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

}
