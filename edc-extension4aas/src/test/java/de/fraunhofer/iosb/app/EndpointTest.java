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
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import static jakarta.ws.rs.core.Response.Status.*;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Not mocking the controllers this endpoint uses, as the mocking/validation
 * process would be pretty complex. Mocking the AAS service instead.
 * Note: Synchronization now happens outside the endpoint, so the
 * selfDescription will be null after requests
 */
@ExtendWith(DependencyInjectionExtension.class)
public class EndpointTest {

    private final URL url = new URL("https://localhost:%s".formatted(getFreePort()));

    private Endpoint testSubject;

    private ServiceRepository serviceRepositoryMock;
    private RegistryRepository registryRepositoryMock;
    private AasController aasControllerMock;

    public EndpointTest() throws MalformedURLException {
    }

    @BeforeEach
    public void setupEndpoint(ServiceExtensionContext context) {
        var monitor = new ConsoleMonitor();
        serviceRepositoryMock = mock(ServiceRepository.class);
        registryRepositoryMock = mock(RegistryRepository.class);
        aasControllerMock = mock(AasController.class);
        testSubject = new Endpoint(serviceRepositoryMock, registryRepositoryMock,
                aasControllerMock, monitor);

        new ConfigurationController(context.getConfig(), monitor).updateConfiguration("{\"edc.aas.allowSelfSignedCertificates\":true}");
    }

    @Test
    void testCreateRegistry() {
        when(registryRepositoryMock.create(any())).thenReturn(true);

        try (var response = testSubject.createRegistry(url.toString())) {
            // Unauthorized because of self-signed certificate
            assertEquals(CREATED.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }
        verify(registryRepositoryMock, times(1)).create(any());
    }

    @Test
    void testCreateService() {
        when(serviceRepositoryMock.create(any())).thenReturn(true);

        try (var response = testSubject.createService(url.toString(), null)) {
            assertEquals(CREATED.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }

        verify(serviceRepositoryMock, times(1)).create(any());
    }

    @Test
    void testRemoveService() {
        try (var response = testSubject.removeService(url.toString())) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }

        verify(serviceRepositoryMock, times(1)).delete(any());
    }

    @Test
    void testRemoveRegistry() {
        doThrow(IllegalAccessError.class).when(aasControllerMock).stopService(any());
        try (var response = testSubject.removeRegistry(url.toString())) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }
        verify(registryRepositoryMock, times(1)).delete(any());
    }

    @Test
    void testCreateRegistryNullValue() {
        when(registryRepositoryMock.create(any())).thenThrow(IllegalAccessError.class);
        try (var response = testSubject.createRegistry(null)) {
            assertEquals(BAD_REQUEST.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }

        assertEquals(0, registryRepositoryMock.getAllEnvironments().size());
    }

    @Test
    void testCreateServiceNullValue() {
        when(serviceRepositoryMock.create(any())).thenThrow(IllegalAccessError.class);
        try (var response = testSubject.createService(null, null)) {
            assertEquals(BAD_REQUEST.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }

        assertEquals(0, serviceRepositoryMock.getAllEnvironments().size());
    }

    @Test
    void testRemoveServiceNullValue() {
        doThrow(IllegalAccessError.class).when(serviceRepositoryMock).delete(any());
        doThrow(IllegalAccessError.class).when(aasControllerMock).stopService(any());
        try (var response = testSubject.removeService(null)) {
            assertEquals(BAD_REQUEST.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }
    }

    @Test
    void testRemoveRegistryNullValue() {
        doThrow(IllegalAccessError.class).when(registryRepositoryMock).delete(any());
        doThrow(IllegalAccessError.class).when(aasControllerMock).stopService(any());
        try (var response = testSubject.removeRegistry(null)) {
            assertEquals(BAD_REQUEST.getStatusCode(),
                    response.getStatusInfo().getStatusCode());
        }
    }

    @Test
    void testPostAasEnvironmentNullEnvironment() throws IOException {
        when(aasControllerMock.startService(any(), any(Integer.class), any())).thenThrow(IllegalAccessError.class);

        try (var response = testSubject.postAasEnvironment(null, "", 0)) {
            assertEquals(BAD_REQUEST.getStatusCode(), response.getStatusInfo().getStatusCode());
        }
    }

    @Test
    void testPostAasEnvironmentNullConfig() throws IOException {
        // This is allowed
        when(aasControllerMock.startService(any(), any(Integer.class), any())).thenReturn(url);
        when(serviceRepositoryMock.create(any())).thenReturn(true);

        try (var response = testSubject.postAasEnvironment(".", null, 0)) {
            assertEquals(CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        }
    }

    @Test
    void testPostAasEnvironmentInvalidPort() throws IOException {
        // This is allowed (in endpoint, aasServiceManager should throw IllegalArgumentException)
        when(aasControllerMock.startService(any(), any(Integer.class), any())).thenThrow(IOException.class);
        when(serviceRepositoryMock.create(any())).thenReturn(true);

        try (var response = testSubject.postAasEnvironment(".", "", -42)) {
            assertEquals(INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatusInfo().getStatusCode());
        }
    }
}
