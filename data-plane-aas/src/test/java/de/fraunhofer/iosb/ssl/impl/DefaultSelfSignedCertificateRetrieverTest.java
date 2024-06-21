package de.fraunhofer.iosb.ssl.impl;

import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.config.CoreConfig;
import de.fraunhofer.iosb.ilt.faaast.service.config.ServiceConfig;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.HttpEndpointConfig;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.internal.MessageBusInternalConfig;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.PersistenceInMemoryConfig;
import de.fraunhofer.iosb.ilt.faaast.service.starter.util.ServiceConfigHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class DefaultSelfSignedCertificateRetrieverTest {

    private static final String LOCALHOST_URL = "https://localhost:";

    private static final String VALID = "https://google.com";
    private static final String EXPIRED = "https://expired.badssl.com";
    private static final String WRONG_HOST = "https://wrong.host.badssl.com";

    @Test
    void getSelfSignedCertificate() throws IOException {
        var port = 12345;
        var serviceConfig = new ServiceConfig.Builder()
                .core(new CoreConfig.Builder().requestHandlerThreadPoolSize(2).build())
                .endpoint(new HttpEndpointConfig.Builder().port(port).build())
                .persistence(PersistenceInMemoryConfig.builder()
                        .initialModelFile(Path.of("src/test/resources/aasEnvironment.json")
                                .toFile())
                        .build())
                .messageBus(new MessageBusInternalConfig())
                .build();
        ServiceConfigHelper.autoComplete(serviceConfig);

        Service service;
        try {
            service = new Service(serviceConfig);
            service.start();
        } catch (Exception faaastServiceException) {
            throw new RuntimeException(faaastServiceException);
        }
        var url = new URL(LOCALHOST_URL + port);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            assertNotNull(certResult.getContent());
        } else {
            fail();
        }

        service.stop();
    }

    @Test
    void getValidCertificate() throws MalformedURLException {
        var url = new URL(VALID);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("trusted"), certResult.getFailureMessages());
        }
    }

    @Test
    void getExpiredCertificate() throws MalformedURLException {
        var url = new URL(EXPIRED);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        } else {
            assertEquals(List.of("expired"), certResult.getFailureMessages());
        }
    }

    @Test
    void getWrongHostCertificate() throws MalformedURLException {
        var url = new URL(WRONG_HOST);
        var certResult = new DefaultSelfSignedCertificateRetriever().getSelfSignedCertificate(url);

        if (certResult.succeeded()) {
            fail();
        }
    }
}