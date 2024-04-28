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

import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.testutils.FileManager;
import de.fraunhofer.iosb.app.testutils.TrustSelfSignedOkHttpClient;
import de.fraunhofer.iosb.app.util.Encoder;
import dev.failsafe.RetryPolicy;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.core.base.EdcHttpClientImpl;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Not mocking the controllers this endpoint uses, as the mocking/validation
 * process would be pretty complex. Mocking the AAS service instead.
 * Note: Synchronization now happens outside the endpoint, so the
 * selfDescription will be null after requests
 */
public class EndpointTest {

    private static final String CONFIG_VALUE_PREFIX = "edc.aas.";
    private static int port;
    private static URL url;

    private Endpoint endpoint;

    private SelfDescriptionRepository selfDescriptionRepo;
    private AasController aasController;

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
    }

    @BeforeEach
    public void setupEndpoint() {
        selfDescriptionRepo = new SelfDescriptionRepository();
        aasController = new AasController(
                new EdcHttpClientImpl(
                        new TrustSelfSignedOkHttpClient()
                                .newBuilder()
                                .build(),
                        RetryPolicy.ofDefaults(),
                        mock(Monitor.class)));
        endpoint = new Endpoint(
                selfDescriptionRepo,
                aasController,
                new ConfigurationController(ConfigFactory.empty()));
    }

    @AfterEach
    public void shutdown() {
        aasController.stopServices();
    }

    @Test
    public void getSelfDescriptionWithParamTest() {
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), endpoint.getSelfDescription(url).getStatus());
    }

    @Test
    public void getSelfDescriptionTest() {
        endpoint.postAasService(url);
        assertEquals("[]", endpoint.getSelfDescription(null).getEntity());
    }

    @Test
    public void changeSingleConfigValueTest() {
        var config = FileManager.loadResource("config.json");
        var configController = new ConfigurationController(ConfigFactory.empty());
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

        // No selfDescription has been added, but the URL will still be periodically
        // polled until the service is deleted via http request again or any AAS output
        // is returned by the URL.
        assertEquals(1, selfDescriptionRepo.getAllSelfDescriptions().size());
    }

    @Test
    public void postAasServiceTest() {
        endpoint.postAasService(url);
        assertNull(selfDescriptionRepo.getSelfDescription(url));
    }

    @Test
    public void postAasEnvironmentTest() {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertNull(selfDescriptionRepo.getSelfDescription(url));

        endpoint.removeAasService(url);
    }

    @Test
    public void removeAasServiceTest() {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, port);

        assertNull(selfDescriptionRepo.getSelfDescription(url));

        endpoint.removeAasService(url);

        assertNull(selfDescriptionRepo.getSelfDescription(url));
    }

    @Test
    public void putAasRequestTest() throws IOException {
        endpoint.postAasService(url);

        endpoint.putAasRequest(new URL(format(url.toString(), "/api/v3.0/submodels/",
                        Encoder.encodeBase64("https://example.com/ids/sm/4445_8090_6012_7409"),
                        "/submodel-elements/GripperUp")),
                FileManager.loadResource("submodelElement.json"));

        // Still null: not synchronized by Synchronizer
        assertNull(selfDescriptionRepo.getSelfDescription(url));
    }

}
