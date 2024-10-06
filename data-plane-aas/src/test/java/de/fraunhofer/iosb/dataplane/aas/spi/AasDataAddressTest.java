package de.fraunhofer.iosb.dataplane.aas.spi;

import de.fraunhofer.iosb.model.aas.AasProvider;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AasDataAddressTest {

    @Test
    void test_build_accessUrlBuiltCorrectly() {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.baseUrl("http://localhost:8080");
        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();

        assertEquals(address.getBaseUrl(), "http://localhost:8080");

        assertEquals(address.getPath(), "/path/to/resource");
    }

    @Test
    void test_build_accessUrlBuiltCorrectlyWithProvider() {
        var addressBuilder = AasDataAddress.Builder.newInstance();

        addressBuilder.baseUrl("http://localhost:8080");
        addressBuilder.aasProvider(new AasProvider((AasAccessUrl) null) {
            @Override
            public URL getAccessUrl() {
                try {
                    return new URL("http://aas-provider:8081");
                } catch (MalformedURLException e) {
                    throw new EdcException("");
                }
            }
        });

        addressBuilder.path("/path/to/resource");

        var address = addressBuilder.build();
        // AasProvider takes precedence over baseUrl
        assertEquals("http://aas-provider:8081", address.getBaseUrl());

        assertEquals(address.getPath(), "/path/to/resource");
    }

}