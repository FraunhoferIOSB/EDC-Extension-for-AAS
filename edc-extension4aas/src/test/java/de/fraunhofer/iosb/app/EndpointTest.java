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
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Not mocking the controllers this endpoint uses, as the mocking/validation
 * process would be pretty complex. Mocking the AAS service instead.
 * Note: Synchronization now happens outside the endpoint, so the
 * selfDescription will be null after requests
 */
public class EndpointTest {

    private static int port;
    private static URL url;

    private Endpoint endpoint;

    private ServiceRepository serviceRepo;
    private RegistryRepository registryRepo;
    private AasController aasController;

    @BeforeAll
    public static void initialize() throws MalformedURLException {
        port = 8080;
        url = new URL("http://localhost:%s".formatted(port));
    }

    @BeforeEach
    public void setupEndpoint() {
        var monitor = new ConsoleMonitor();
        serviceRepo = new ServiceRepository();
        registryRepo = new RegistryRepository();
        aasController = new AasController(new AasServiceRegistry(Set.of()), monitor);
        endpoint = new Endpoint(
                serviceRepo,
                registryRepo,
                aasController,
                monitor);
    }

    @AfterEach
    public void shutdown() {
        aasController.stopServices();
    }

    @Test
    public void postFalseAasServiceTest() throws MalformedURLException {
        assertEquals(0, serviceRepo.getAllEnvironments().size());

        endpoint.createService(new URL("http://example.com/aas"));

        // No selfDescription has been added, but the URL will still be periodically
        // polled until the service is deleted via http request again or any AAS output
        // is returned by the URL.
        assertEquals(1, serviceRepo.getAllEnvironments().size());
    }

    @Test
    public void postAasServiceTest() {
        endpoint.removeService(url);
        assertNull(serviceRepo.getEnvironment(url));
    }

    @Test
    public void postAasEnvironmentTest() {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, String.valueOf(port));

        assertNull(serviceRepo.getEnvironment(url));

        endpoint.removeService(url);
    }

    @Test
    public void removeAasServiceTest() {
        endpoint.postAasEnvironment("src/test/resources/aasEnvironment.json", null, String.valueOf(port));

        assertNull(serviceRepo.getEnvironment(url));

        endpoint.removeService(url);

        assertNull(serviceRepo.getEnvironment(url));
    }
}
