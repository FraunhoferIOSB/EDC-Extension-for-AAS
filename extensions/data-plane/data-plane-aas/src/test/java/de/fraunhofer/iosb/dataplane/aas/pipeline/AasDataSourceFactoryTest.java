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
package de.fraunhofer.iosb.dataplane.aas.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.aas.lib.AasDataProcessor;
import de.fraunhofer.iosb.aas.lib.AasDataProcessorFactory;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress.PROXY_OPERATION;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class AasDataSourceFactoryTest {

    private final AasDataProcessorFactory mockedDataProcessorFactory = mock(AasDataProcessorFactory.class);
    private AasDataSourceFactory testSubject;


    @BeforeEach
    void setUp() {
        testSubject = new AasDataSourceFactory(new ConsoleMonitor().withPrefix(this.getClass().getSimpleName()),
                mockedDataProcessorFactory);
    }


    @Disabled("Changed functionality")
    @Test
    void test_createSource_differentTargetDataAddressType() throws Exception {
        var mockedDataFlowStartMessage = mock(DataFlowStartMessage.class);
        var accessUri = new URI("https://localhost:1234");
        var mockedProcessor = mock(AasDataProcessor.class);
        when(mockedProcessor.getFromAas(any()))
                .thenReturn(new Response.Builder()
                        .code(200)
                        .request(new Request.Builder()
                                .url(accessUri.toURL())
                                .build())
                        .protocol(Protocol.HTTP_1_1)
                        .message("")
                        .body(ResponseBody.create("{\"test\": \"ok\"}".getBytes(StandardCharsets.UTF_8), okhttp3.MediaType.get("application/json")))
                        .build());
        when(mockedDataProcessorFactory.processorFor(accessUri.toString())).thenReturn(Result.success(mockedProcessor));

        when(mockedDataFlowStartMessage.getSourceDataAddress())
                .thenReturn(AasDataAddress.Builder.newInstance()
                        .baseUrl(accessUri.toString())
                        .build());

        // Destination of not type AasData
        when(mockedDataFlowStartMessage.getDestinationDataAddress())
                .thenReturn(DataAddress.Builder.newInstance()
                        .type("HttpData")
                        .property("operation", new ObjectMapper().writeValueAsString(new DefaultOperation.Builder()
                                .inoutputVariables(new DefaultOperationVariable.Builder().build())
                                .inputVariables(new DefaultOperationVariable.Builder().build())
                                .outputVariables(new DefaultOperationVariable.Builder().build())
                                .build()))
                        .build());
        when(mockedDataFlowStartMessage.getId()).thenReturn("testId");

        try (var source = testSubject.createSource(mockedDataFlowStartMessage)) {
            source.openPartStream();
        }

        verify(mockedDataProcessorFactory, times(1)).processorFor(accessUri.toString());

        // Here is the actual tested feat
        verify(mockedProcessor).getFromAas(argThat(aasDataAddress -> aasDataAddress.hasProperty(PROXY_OPERATION)));
    }
}
