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
package de.fraunhofer.iosb.ilt.dataplane.aas.pipeline;

import de.fraunhofer.iosb.ilt.aas.lib.AasDataProcessor;
import de.fraunhofer.iosb.ilt.aas.lib.AasDataProcessorFactory;
import de.fraunhofer.iosb.ilt.aas.lib.spi.AasDataAddress;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
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