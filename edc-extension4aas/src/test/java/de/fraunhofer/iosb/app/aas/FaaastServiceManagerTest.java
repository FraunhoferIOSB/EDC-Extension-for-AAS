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
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.util.HttpRestClient;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;

public class FaaastServiceManagerTest {

    private AssetAdministrationShellServiceManager faaastServiceManager;

    @BeforeAll
    public static void initializeLogger() {
        Logger.getInstance().setMonitor(mock(Monitor.class));
    }

    @BeforeEach
    public void initializeFaaastServiceManager() {
        faaastServiceManager = new FaaastServiceManager();
    }

    @Test
    public void startServiceTest() throws IOException, URISyntaxException {
        var url = startService();
        var restClient = new HttpRestClient(new OkHttpClient());

        assertEquals(Response.Status.OK.getStatusCode(), restClient.get(url.toURI().resolve("/shells").toURL()).code());
    }

    @Test
    public void startServiceFalsePathTest() throws IOException {
        assertEquals(null, faaastServiceManager.startService(Path.of("fake-path"), 12345));
    }

    @Test
    public void startServiceFalsePortTest() throws IOException {
        assertEquals(null,
                faaastServiceManager.startService(Path.of("./src/test/resources/aasEnvironment.json"), -800));
    }

    @Test
    public void startServiceFalsePortConfigPathTest() throws IOException {
        assertEquals(null, faaastServiceManager.startService(Path.of("./src/test/resources/aasEnvironment.json"),
                Path.of("./src/test/resources/aasEnvironment.json")));
    }

    @Test
    public void stopServicesEmptyRepositoryTest() throws IOException {
        try {
            faaastServiceManager.stopServices();
        } catch (Exception failed) {
            fail("This operation must not fail");
        }
    }

    @Test
    public void stopServiceEmptyRepositoryTest() throws IOException {
        try {
            faaastServiceManager.stopService(new URL("http://does-not-exist.com:1234/aas"));
            fail("This operation should fail");
        } catch (IllegalArgumentException expectedException) {
            return;
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
    public void stopServices(){
        faaastServiceManager.stopServices();
    }
}
