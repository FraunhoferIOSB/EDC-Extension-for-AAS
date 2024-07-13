package de.fraunhofer.iosb.aas;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.ilt.faaast.service.Service;
import de.fraunhofer.iosb.ilt.faaast.service.exception.EndpointException;
import de.fraunhofer.iosb.ilt.faaast.service.exception.MessageBusException;
import de.fraunhofer.iosb.testutils.SelfSignedCertificateProvider;
import dev.failsafe.RetryPolicy;
import okhttp3.OkHttpClient;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.edc.http.client.EdcHttpClientImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

class AasDataProcessorTest {

    private String aasUrl;

    AasDataProcessor testSubject;
    Service aasService;

    @BeforeEach
    void setUp() throws MessageBusException, EndpointException {
        var edcHttpClient = new EdcHttpClientImpl(new OkHttpClient(), RetryPolicy.ofDefaults(), new ConsoleMonitor());
        testSubject = new AasDataProcessor(edcHttpClient);
        var port = 52789;
        aasService = SelfSignedCertificateProvider.getService(port);
        aasService.start();
        aasUrl = "https://localhost:%s".formatted(port);
    }

    @Test
    void testSendAddressOnly() throws IOException {
        try (var response = testSubject.send(null)) {
            response.code();
        }
    }

    @Test
    void testSendWithBody() {
    }

    @Test
    void testSendWithPart() {
    }

    @AfterEach
    void tearDown() {
        aasService.stop();
    }

    private AasDataAddress getAddress() {
        return AasDataAddress.Builder.newInstance()
                .baseUrl(aasUrl)
                .method("GET")
                .referenceChain(
                        new DefaultReference.Builder()
                                .keys(List.of(
                                        new DefaultKey.Builder()
                                                .type(KeyTypes.ASSET_ADMINISTRATION_SHELL)
                                                .value("xyz")
                                                .build()
                                ))
                                .build()
                )
                .build();
    }
}