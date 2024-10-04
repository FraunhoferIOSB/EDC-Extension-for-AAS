package de.fraunhofer.iosb.app.model.aas.service;

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ServiceTest {

    @Test
    void testWithReturnsDifferentObjectReference() throws MalformedURLException {
        Service myService = new Service(new URL("http://aas-access-url"));

        var environment = Asset.Builder.newInstance().build();
        Service withedService = myService.with(environment);

        // Compares reference
        assertNotSame(myService, withedService);
        // The "old" service should also not copy the new one's environment
        assertNotEquals(environment, myService.environment());
    }

}