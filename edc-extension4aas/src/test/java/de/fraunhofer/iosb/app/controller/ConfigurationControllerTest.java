package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.app.testutils.FileManager;
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