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

import de.fraunhofer.iosb.aas.impl.AllAasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ssl.impl.DefaultSelfSignedCertificateRetriever;
import dev.failsafe.RetryPolicy;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FaaastServiceManagerTest {

    private AssetAdministrationShellServiceManager faaastServiceManager;

    @BeforeEach
    public void initializeFaaastServiceManager() {
        faaastServiceManager = new FaaastServiceManager(new ConsoleMonitor());
    }

    @Test
    public void startServiceTest() throws IOException, URISyntaxException {
        var url = startService();
        var factory = new AllAasDataProcessorFactory(
                new DefaultSelfSignedCertificateRetriever(),
                new OkHttpClient(),
                RetryPolicy.ofDefaults(),
                new ConsoleMonitor());
        try (var response = factory.processorFor(url.toString()).getContent()
                .send(AasDataAddress.Builder.newInstance()
                                .method(HttpMethod.GET)
                                .baseUrl(url.toURI().resolve("/api/v3.0/shells").toString())
                                .build(),
                        null, null)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.code());
        }
    }

    @Test
    public void startServiceFalsePathTest() throws IOException {
        try {
            faaastServiceManager.startService(Path.of("fake-path"), 12345);
            fail("EdcException should have been thrown");
        } catch (EdcException expected) {
        }
    }

    @Test
    public void startServiceOverwritePortTest() throws IOException {
        Path testPath = Path.of("./src/test/resources/aasEnvironment.json");
        // Fa³st config path irrelevant, configHelper creates new config with port 8080
        var response = faaastServiceManager.startService(testPath, testPath, 12345);
        assertEquals(12345, response.getPort());
    }

    @Test
    public void startServiceFalsePortTest() throws IOException {
        try {
            faaastServiceManager.startService(Path.of("./src/test/resources/aasEnvironment.json"), -800);
            fail("EdcException should have been thrown");
        } catch (EdcException expected) {
        }
    }

    @Test
    public void stopServicesEmptyRepositoryTest() {
        try {
            faaastServiceManager.stopServices();
        } catch (Exception failed) {
            fail("This operation must not fail");
        }
    }

    @Test
    public void stopServiceEmptyRepositoryTest() {
        try {
            faaastServiceManager.stopService(new URL("http://does-not-exist.com:1234/aas"));
        } catch (Exception unexpectedException) {
            fail();
        }
    }

    @Test
    public void stopServiceTest() throws IOException {
        faaastServiceManager.stopService(startService());
    }

    private URL startService() throws IOException {
        return faaastServiceManager.startService(Path.of("./src/test/resources/aasEnvironment.json"),
                8080);
    }

    @AfterEach
    public void stopServices() {
        faaastServiceManager.stopServices();
    }
}
