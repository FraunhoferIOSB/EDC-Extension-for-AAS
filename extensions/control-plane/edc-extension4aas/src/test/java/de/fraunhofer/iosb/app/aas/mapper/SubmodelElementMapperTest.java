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
package de.fraunhofer.iosb.app.aas.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.aas.mapper.environment.referable.SubmodelElementMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static de.fraunhofer.iosb.app.testutils.AasCreator.getProperty;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SubmodelElementMapperTest {

    private SubmodelElementMapper testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new SubmodelElementMapper();
    }


    @Disabled("TODO decide if operation variables are advertised (see SubmodelElementMapper.java")
    @Test
    void test_map_operation() throws JsonProcessingException {
        var mockParent = new DefaultReference.Builder()
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.SUBMODEL)
                        .value("sm-id")
                        .build())
                .build();
        List<OperationVariable> mockInputVariables = List.of(
                new DefaultOperationVariable.Builder()
                        .value(getProperty("input1"))
                        .build(),
                new DefaultOperationVariable.Builder()
                        .value(getProperty("input2"))
                        .build());
        List<OperationVariable> mockInoutputVariables = List.of(
                new DefaultOperationVariable.Builder()
                        .value(getProperty("inout1")).build(),
                new DefaultOperationVariable.Builder()
                        .value(getProperty("inout2")).build());

        List<OperationVariable> mockOutputVariables = List.of(
                new DefaultOperationVariable.Builder()
                        .value(getProperty("out1")).build(),
                new DefaultOperationVariable.Builder()
                        .value(getProperty("out2")).build());

        var mockOperation = new DefaultOperation.Builder()
                .idShort("test-operation-id-short")
                .inputVariables(mockInputVariables)
                .inoutputVariables(mockInoutputVariables)
                .outputVariables(mockOutputVariables)
                .build();

        var resultAsset = testSubject.map(mockParent, mockOperation, mockService());

        assertEquals(mockInputVariables, resultAsset.getProperty(AAS_V30_NAMESPACE + "Operation/" + "inputVariables"));
        assertEquals(mockInoutputVariables, resultAsset.getProperty(AAS_V30_NAMESPACE + "Operation/" + "inoutputVariables"));
        assertEquals(mockOutputVariables, resultAsset.getProperty(AAS_V30_NAMESPACE + "Operation/" + "outputVariables"));
    }

    private Service mockService() {
        try {
            return new Service.Builder().withUrl(new URL("https://test-url:1234/api/v3.0")).build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}