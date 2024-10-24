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
package de.fraunhofer.iosb.client.datatransfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.model.aas.service.Service;
import jakarta.ws.rs.core.Response;
import org.eclipse.digitaltwin.aas4j.v3.model.Operation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.fraunhofer.iosb.client.datatransfer.DataTransferController.OPERATION_FIELD;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataTransferControllerTest {
    private DataTransferController testSubject;
    private static URL url;
    private static final String agreementId = UUID.randomUUID().toString();

    private TransferProcessManager mockTransferProcessManager = mock(TransferProcessManager.class);

    @BeforeEach
    public void setup() throws IOException {
        int port = 8080;
        url = new URL(format("http://localhost:%s", port));
        testSubject = new DataTransferController(
                new ConsoleMonitor().withPrefix("DataTransferControllerTest"),
                mockConfig(),
                mock(WebService.class),
                mock(PublicApiManagementService.class),
                mockTransferProcessManager,
                mock(TransferProcessObservable.class),
                () -> "localhost");
    }

    private Config mockConfig() {
        return ConfigFactory.fromMap(
                Map.of(
                        "edc.dsp.callback.address", "http://localhost:4321/dsp",
                        "web.http.port", "8080",
                        "web.http.path", "/api"));
    }

    @Test
    void test_getData_correctlySerializeOperation() throws JsonProcessingException {
        Operation operation = getOperation();
        var nnneObjectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        var operationString = nnneObjectMapper.writeValueAsString(operation);
        DataAddress dataSinkAddress = getDataAddress(operation);

        testSubject.getData(url, agreementId, dataSinkAddress);
        // Verify that operation is serialized before sending it to provider
        verify(mockTransferProcessManager).initiateConsumerRequest(argThat(request ->
                operationString.equals(request.getDataDestination().getStringProperty("operation"))));
    }

    private static DataAddress getDataAddress(Operation operation) {
        return DataAddress.Builder.newInstance()
                .type("my-da-type")
                .property("hello", "world")
                .property("foo", new Object() {
                    final String x = "bar";
                })
                .property("baz", 1)
                .property(OPERATION_FIELD, operation)
                .build();
    }

    private static Operation getOperation() {
        return new DefaultOperation.Builder()
                .category("cat")
                .description(new DefaultLangStringTextType.Builder()
                        .language("en")
                        .text("helloworld")
                        .build())
                .inputVariables(new DefaultOperationVariable.Builder()
                        .value(new DefaultProperty.Builder()
                                .value("seven")
                                .build())
                        .build())
                .inoutputVariables(List.of(
                        new DefaultOperationVariable.Builder()
                                .value(new DefaultProperty.Builder()
                                        .value("eight")
                                        .build())
                                .build(),
                        new DefaultOperationVariable.Builder()
                                .value(new DefaultProperty.Builder()
                                        .value("inout")
                                        .build())
                                .build()))
                .outputVariables(List.of(
                        new DefaultOperationVariable.Builder()
                                .value(new DefaultProperty.Builder()
                                        .value("nine")
                                        .build())
                                .build(),
                        new DefaultOperationVariable.Builder()
                                .value(new DefaultProperty.Builder()
                                        .value("out")
                                        .build())
                                .build()
                ))
                .build();
    }

    @Test
    void getDataTest() {
        var dataAddress = AasDataAddress.Builder.newInstance().aasProvider(new Service(url)).build();
        try (var response = testSubject.getData(url, agreementId, dataAddress)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }
    }
}