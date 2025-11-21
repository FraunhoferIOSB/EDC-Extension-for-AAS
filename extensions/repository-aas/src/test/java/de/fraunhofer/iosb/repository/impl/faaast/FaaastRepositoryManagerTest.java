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
package de.fraunhofer.iosb.repository.impl.faaast;

import de.fraunhofer.iosb.model.config.impl.faaast.FaaastRepositoryConfig;
import de.fraunhofer.iosb.model.context.repository.AasRepositoryContext;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


class FaaastRepositoryManagerTest {

    private FaaastRepositoryManager testSubject;


    @BeforeEach
    public void initializeFaaastServiceManager() {
        testSubject = new FaaastRepositoryManager(new ConsoleMonitor(), () -> "faaast");
    }


    @Test
    public void startServiceTest() throws IOException {
        testSubject = new FaaastRepositoryManager(new ConsoleMonitor(), () -> "localhost");

        var uri = startRepository();

        var edcHttpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());

        try (var response = edcHttpClient.execute(new Request.Builder()
                .url(uri.toString() + "/shells")
                .method("GET", null)
                .build())) {
            assertEquals(200, response.code());
        }
    }


    @Test
    public void startServiceFalsePathTest() {
        try {
            var freePort = getFreePort();
            testSubject.startRepository(FaaastRepositoryConfig.Builder.newInstance()
                    .configPath(Path.of("fake-path"))
                    .port(freePort)
                    .build());
            fail("Exception should have been thrown");
        }
        catch (IllegalArgumentException expected) {
        }
    }


    @Test
    public void startServiceOverwritePortTest() {
        Path testPath = Path.of("./src/test/resources/aasEnvironment.json");
        // Fa³st config path irrelevant, configHelper creates new config with port 443
        var freePort = getFreePort();
        AasRepositoryContext response = testSubject.startRepository(
                FaaastRepositoryConfig.Builder.newInstance()
                        .model(testPath)
                        .port(freePort)
                        .build());
        assertEquals(freePort, response.getUri().getPort());
    }


    @Test
    public void stopServicesEmptyRepositoryTest() {
        try {
            testSubject.stopAll();
        }
        catch (Exception failed) {
            fail("This operation must not fail");
        }
    }


    @Test
    public void stopServiceEmptyRepositoryTest() {
        try {
            testSubject.stopRepository(new URI("http://does-not-exist.com:1234/aas"));
        }
        catch (Exception unexpectedException) {
            fail();
        }
    }


    @Test
    public void stopServiceTest() {
        testSubject.stopRepository(startRepository());
    }


    private URI startRepository() {
        var config = FaaastRepositoryConfig.Builder.newInstance().model(
                        Path.of("./src/test/resources/aasEnvironment.json"))
                .port(getFreePort())
                .ssl(false)
                .build();

        return testSubject.startRepository(config).getUri();
    }


    @AfterEach
    public void stopServices() {
        testSubject.stopAll();
    }
}
