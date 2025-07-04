package de.fraunhofer.iosb.dataplane.aas.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.aas.lib.AasDataProcessor;
import de.fraunhofer.iosb.aas.lib.AasDataProcessorFactory;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.aas.lib.spi.AasDataAddress;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.DataFlowStartMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class AasDataSourceFactoryTest {

    private final AasDataProcessorFactory mockedDataProcessorFactory = mock(AasDataProcessorFactory.class);
    private AasDataSourceFactory testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new AasDataSourceFactory(
                new ConsoleMonitor().withPrefix(this.getClass().getSimpleName()),
                mockedDataProcessorFactory);
    }

    @Test
    void test_createSource_differentTargetDataAddressType() throws Exception {
        var mockedDataFlowStartMessage = mock(DataFlowStartMessage.class);
        var accessUrl = new URL("https://localhost:1234");
        var mockedProcessor = mock(AasDataProcessor.class);
        when(mockedProcessor.send(any()))
                .thenReturn(new Response.Builder()
                        .code(200)
                        .request(new Request.Builder()
                                .url(accessUrl)
                                .build())
                        .protocol(Protocol.HTTP_1_1)
                        .message("")
                        .body(ResponseBody.create("{\"test\": \"ok\"}".getBytes(StandardCharsets.UTF_8),
                                okhttp3.MediaType.get("application/json")))
                        .build());
        when(mockedDataProcessorFactory.processorFor(accessUrl)).thenReturn(Result.success(mockedProcessor));

        when(mockedDataFlowStartMessage.getSourceDataAddress())
                .thenReturn(AasDataAddress.Builder.newInstance()
                        .aasProvider(new Service(accessUrl))
                        .build());

        // Destination of not type AasData
        when(mockedDataFlowStartMessage.getDestinationDataAddress())
                .thenReturn(DataAddress.Builder.newInstance().type("HttpData")
                        .property("operation", new ObjectMapper().writeValueAsString(
                                new DefaultOperation.Builder()
                                        .inoutputVariables(new DefaultOperationVariable.Builder().build())
                                        .inputVariables(new DefaultOperationVariable.Builder().build())
                                        .outputVariables(new DefaultOperationVariable.Builder().build())
                                        .build()))
                        .build());
        when(mockedDataFlowStartMessage.getId()).thenReturn("testId");

        try (var source = testSubject.createSource(mockedDataFlowStartMessage)) {
            var streamResult = source.openPartStream();
        }

        verify(mockedDataProcessorFactory, times(1)).processorFor(accessUrl);

        // Here is the actual tested feat
        verify(mockedProcessor).send(argThat(AasDataAddress::isOperation));
    }
}