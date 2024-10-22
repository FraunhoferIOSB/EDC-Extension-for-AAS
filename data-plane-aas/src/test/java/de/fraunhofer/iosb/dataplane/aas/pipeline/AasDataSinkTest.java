package de.fraunhofer.iosb.dataplane.aas.pipeline;

import de.fraunhofer.iosb.aas.AasDataProcessor;
import de.fraunhofer.iosb.aas.AasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.connector.dataplane.spi.pipeline.StreamResult;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.eclipse.edc.util.io.Ports.getFreePort;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AasDataSinkTest {

    static URL destinationUrl;

    static {
        try {
            destinationUrl = new URL("https://localhost:%s/api/v3.0".formatted(getFreePort()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    AasDataSink testSubject;

    AasDataProcessorFactory mockAasDataprocessorFactory = mock(AasDataProcessorFactory.class);
    AasDataAddress mockAasDataAddress = mock(AasDataAddress.class);

    @BeforeEach
    void setUp() {
        testSubject = AasDataSink.Builder.newInstance()
                .aasManipulator(mockAasDataprocessorFactory)
                .aasDataAddress(mockAasDataAddress)
                .monitor(new ConsoleMonitor().withPrefix(this.getClass().getSimpleName()))
                .build();
    }

    @Test
    void test_transfer_normalBehaviour() throws IOException {
        AasDataSource mockDataSource = mock(AasDataSource.class);

        // Source data mock
        AasPart mockAasPart = mock(AasPart.class);
        when(mockAasPart.openStream()).thenReturn(new ByteArrayInputStream("test-body".getBytes(StandardCharsets.UTF_8)));
        when(mockDataSource.openPartStream()).thenReturn(StreamResult.success(Stream.of(mockAasPart)));

        // Destination address mock
        when(mockAasDataAddress.getAccessUrl()).thenReturn(Result.success(destinationUrl));

        AasDataProcessor mockAasDataProcessor = mock(AasDataProcessor.class);
        when(mockAasDataprocessorFactory.processorFor(any())).thenReturn(Result.success(mockAasDataProcessor));
        var mockedResponse = mock(Response.class);
        when(mockedResponse.body()).thenReturn(ResponseBody.create("", MediaType.get("application/json")));
        when(mockAasDataProcessor.send(any(), eq(mockAasPart))).thenReturn(mockedResponse);
        testSubject.transfer(mockDataSource);

        verify(mockAasDataProcessor, atLeastOnce()).send(any(), eq(mockAasPart));
    }
}