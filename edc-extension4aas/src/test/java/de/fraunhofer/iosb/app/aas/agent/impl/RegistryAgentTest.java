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
package de.fraunhofer.iosb.app.aas.agent.impl;

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.model.aas.registry.Registry;
import de.fraunhofer.iosb.model.aas.service.Service;
import de.fraunhofer.iosb.registry.AasServiceRegistry;
import de.fraunhofer.iosb.ssl.impl.NoOpSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Optional;

import static de.fraunhofer.iosb.api.model.HttpMethod.GET;
import static de.fraunhofer.iosb.app.pipeline.PipelineFailure.Type.WARNING;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getEmptyShellDescriptor;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getEmptySubmodelDescriptor;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getShellDescriptor;
import static de.fraunhofer.iosb.app.testutils.RegistryElementCreator.getSubmodelDescriptor;
import static de.fraunhofer.iosb.app.testutils.StringMethods.resultOf;
import static de.fraunhofer.iosb.model.aas.registry.Registry.SHELL_DESCRIPTORS_PATH;
import static de.fraunhofer.iosb.model.aas.registry.Registry.SUBMODEL_DESCRIPTORS_PATH;
import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

class RegistryAgentTest {
    private static final int PORT = getFreePort();
    private static ClientAndServer mockServer;
    private static RegistryAgent testSubject;
    private final URL mockServerUrl = new URL("http://localhost:%s".formatted(PORT));

    RegistryAgentTest() throws MalformedURLException {
    }

    @BeforeAll
    static void setUp() {
        testSubject = new RegistryAgent(
                new AllAasDataProcessorFactory(
                        new NoOpSelfSignedCertificateRetriever(),
                        new OkHttpClient(),
                        RetryPolicy.ofDefaults(),
                        new ConsoleMonitor()
                ),
                new AasServiceRegistry(
                        new HashSet<>())
        );

        mockServer = startClientAndServer(PORT);
    }

    @AfterAll
    static void shutdown() {
        mockServer.stop();
    }

    @AfterEach
    void tearDown() {
        mockServer.reset();
    }

    @Test
    void test_apply_emptyButNonNullShellDescriptor() throws SerializationException, MalformedURLException {
        var shellDescriptor = getEmptyShellDescriptor();

        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SHELL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(shellDescriptor)));

        mockEmptySubmodelRequest();

        var result = testSubject.apply(new Registry(mockServerUrl));

        assertTrue(result.succeeded());

        var bodyAsEnvironment = result.getContent();

        assertEquals(1, bodyAsEnvironment.size());

        // We know the endpoint url from getEmptyShellDescriptor()...
        var env = bodyAsEnvironment.get(new Service(new URL("https://localhost:12345")));

        var shell = Optional.ofNullable(env.getAssetAdministrationShells().get(0)).orElseThrow();

        assertEquals(shellDescriptor.getIdShort(), shell.getIdShort());
        assertEquals(shellDescriptor.getId(), shell.getId());
        assertEquals(shellDescriptor.getAdministration(), shell.getAdministration());
        assertEquals(shellDescriptor.getAssetType(), shell.getAssetInformation().getAssetType());
        assertEquals(shellDescriptor.getAssetKind(), shell.getAssetInformation().getAssetKind());
    }

    @Test
    void testApplyShellDescriptor() throws SerializationException, MalformedURLException {
        var shellDescriptor = getShellDescriptor();

        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SHELL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(shellDescriptor)));

        mockEmptySubmodelRequest();

        var result = testSubject.apply(new Registry(mockServerUrl));

        assertTrue(result.succeeded());

        var bodyAsEnvironment = result.getContent();

        assertEquals(1, bodyAsEnvironment.size());

        var env = bodyAsEnvironment.get(new Service(new URL("https://localhost:12345")));

        var shell = Optional.ofNullable(env.getAssetAdministrationShells().get(0)).orElseThrow();

        assertEquals(shellDescriptor.getIdShort(), shell.getIdShort());
        assertEquals(shellDescriptor.getId(), shell.getId());
        assertEquals(shellDescriptor.getAdministration(), shell.getAdministration());
        assertEquals(shellDescriptor.getAssetType(), shell.getAssetInformation().getAssetType());
        assertEquals(shellDescriptor.getAssetKind(), shell.getAssetInformation().getAssetKind());
    }

    @Test
    void testApplyEmptySubmodelDescriptor() throws SerializationException, MalformedURLException {
        var submodelDescriptor = getEmptySubmodelDescriptor();

        mockEmptyShellRequest();

        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SUBMODEL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(submodelDescriptor)));

        var result = testSubject.apply(new Registry(mockServerUrl));

        assertTrue(result.succeeded());

        var bodyAsEnvironment = result.getContent();

        assertEquals(1, bodyAsEnvironment.size());

        var submodel = Optional.ofNullable(Optional
                        .ofNullable(bodyAsEnvironment.get(new Service(new URL("https://localhost:12345"))))
                        .orElseThrow()
                        .getSubmodels()
                        .get(0))
                .orElseThrow();

        assertEquals(submodelDescriptor.getIdShort(), submodel.getIdShort());
        assertEquals(submodelDescriptor.getId(), submodel.getId());
        assertEquals(submodelDescriptor.getAdministration(), submodel.getAdministration());
        assertEquals(submodelDescriptor.getDescription(), submodel.getDescription());
        assertEquals(submodelDescriptor.getSemanticId(), submodel.getSemanticId());
    }

    @Test
    void testApplySubmodelDescriptor() throws SerializationException, MalformedURLException {
        var submodelDescriptor = getSubmodelDescriptor();

        mockEmptyShellRequest();

        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SUBMODEL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(submodelDescriptor)));

        var result = testSubject.apply(new Registry(mockServerUrl));

        assertTrue(result.succeeded());

        var bodyAsEnvironment = result.getContent();

        assertEquals(1, bodyAsEnvironment.size());

        var submodel = Optional.ofNullable(Optional
                        .ofNullable(bodyAsEnvironment.get(new Service(new URL("https://localhost:12345"))))
                        .orElseThrow()
                        .getSubmodels()
                        .get(0))
                .orElseThrow();

        assertEquals(submodelDescriptor.getIdShort(), submodel.getIdShort());
        assertEquals(submodelDescriptor.getId(), submodel.getId());
        assertEquals(submodelDescriptor.getAdministration(), submodel.getAdministration());
        assertEquals(submodelDescriptor.getDescription(), submodel.getDescription());
        assertEquals(submodelDescriptor.getSemanticId(), submodel.getSemanticId());
    }

    @Test
    void testApplyNotActuallyRegistry() throws MalformedURLException {
        var result = testSubject.apply(new Registry(new URL("https://example.com")));

        assertTrue(result.failed());
        assertEquals(WARNING, result.getFailure().getFailureType());
    }

    @Test
    void testApplyUnreachableRegistry() throws MalformedURLException {
        var result = testSubject.apply(new Registry(new URL("http://anonymous.invalid")));

        assertTrue(result.failed());
        assertEquals(WARNING, result.getFailure().getFailureType());
    }

    private void mockEmptyShellRequest() throws SerializationException {
        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SHELL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(null)));
    }

    private void mockEmptySubmodelRequest() throws SerializationException {
        mockServer.when(request()
                        .withMethod(GET.toString())
                        .withPath("/%s".formatted(SUBMODEL_DESCRIPTORS_PATH)))
                .respond(HttpResponse.response()
                        .withBody(resultOf(null)));
    }
}