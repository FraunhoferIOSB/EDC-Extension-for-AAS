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

import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.app.controller.AasController;
import de.fraunhofer.iosb.app.controller.ConfigurationController;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.model.ids.SelfDescriptionRepository;
import de.fraunhofer.iosb.app.testutils.FileManager;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        var monitor = new ConsoleMonitor();
        selfDescriptionRepo = new SelfDescriptionRepository();
        aasController = new AasController(monitor, new AasDataProcessorFactory(new NoOpSelfSignedCertificateRetriever()));
        endpoint = new Endpoint(
                selfDescriptionRepo,
                aasController,
                new ConfigurationController(ConfigFactory.empty(), monitor), monitor);
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
        try (var response = endpoint.postAasService(url)) {
            assertEquals("[]", endpoint.getSelfDescription(null).getEntity());
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void changeSingleConfigValueTest() {
        var config = FileManager.loadResource("config.json");
        var configController = new ConfigurationController(ConfigFactory.empty(), new ConsoleMonitor());
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
        assertEquals(0, selfDescriptionRepo.getAllSelfDescriptions().size());

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
}
