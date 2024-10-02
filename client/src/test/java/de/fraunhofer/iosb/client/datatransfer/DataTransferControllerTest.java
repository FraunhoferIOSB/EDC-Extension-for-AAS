package de.fraunhofer.iosb.client.datatransfer;

import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataTransferControllerTest {
    private DataTransferController testSubject;
    private Monitor monitor;

    private static URL url;

    @BeforeEach
    public void setup() throws IOException {
        int port = 8080;
        url = new URL(format("http://localhost:%s", port));
        monitor = new ConsoleMonitor().withPrefix("DataTransferControllerTest");
        testSubject = new DataTransferController(
                monitor,
                mockConfig(),
                mock(WebService.class),
                mock(PublicApiManagementService.class),
                mockTransferProcessManager(),
                () -> "localhost");
    }

    private TransferProcessManager mockTransferProcessManager() {
        StatusResult<TransferProcess> mockStatusResult = StatusResult.failure(ResponseStatus.FATAL_ERROR);

        var mockTransferProcessManager = mock(TransferProcessManager.class);
        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
        return mockTransferProcessManager;
    }

    private Config mockConfig() {
        return ConfigFactory.fromMap(
                Map.of(
                        "edc.dsp.callback.address", "http://localhost:4321/dsp",
                        "web.http.port", "8080",
                        "web.http.path", "/api"));
    }

    @Test
    public void getDataTest() {
        var dataAddress = AasDataAddress.Builder.newInstance().baseUrl(url.toString()).build();
        try (var response = testSubject.getData(url, "test-agreement-id", dataAddress)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }
}