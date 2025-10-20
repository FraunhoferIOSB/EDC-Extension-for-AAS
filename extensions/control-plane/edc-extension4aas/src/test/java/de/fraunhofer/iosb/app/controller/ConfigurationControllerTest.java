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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.aas.test.FileManager;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationControllerTest {
    private static final String CONFIG_VALUE_PREFIX = "edc.aas.";

    private ConfigurationController testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new ConfigurationController(ConfigFactory.empty(), new ConsoleMonitor());
    }

    @Test
    public void changeSingleConfigValueTest() {
        var config = FileManager.loadResource("config.json");
        testSubject.updateConfiguration(config);

        testSubject.updateConfiguration("{\"" + CONFIG_VALUE_PREFIX + "exposeselfdescription\":true}");

        assertTrue(Configuration.getInstance().isExposeSelfDescription());
        assertEquals(123456, Configuration.getInstance().getSyncPeriod());
    }

    @Test
    public void putWrongConfigValueTest() {
        try (var response = testSubject.updateConfiguration("{\"" + CONFIG_VALUE_PREFIX + "falseconfigvalue\":12}")) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }
    }

}