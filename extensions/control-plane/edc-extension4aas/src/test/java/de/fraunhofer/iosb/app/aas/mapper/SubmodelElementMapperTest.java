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
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultBlob;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperation;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultOperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultProperty;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodelElementList;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static de.fraunhofer.iosb.app.aas.mapper.environment.referable.SubmodelElementMapper.SMC_CHILDREN_LOCATION;
import static de.fraunhofer.iosb.app.testutils.AasCreator.getProperty;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_PREFIX;
import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmodelElementMapperTest {

    private SubmodelElementMapper testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new SubmodelElementMapper();
    }


    @Disabled("TODO decide if operation variables are advertised (see SubmodelElementMapper.java")
    @Test
    void test_map_operation() {
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

    @Test
    void map_submodelElementList_elementsWithoutIdShort() {
        Service mockService = mockService();

        String parentSubmodelId = UUID.randomUUID().toString();

        Reference parentReference =
                new DefaultReference.Builder()
                        .type(ReferenceTypes.MODEL_REFERENCE)
                        .keys(new DefaultKey.Builder()
                                .type(KeyTypes.SUBMODEL)
                                .value(parentSubmodelId)
                                .build())
                        .build();

        List<SubmodelElement> listElements = List.of(
                new DefaultProperty.Builder().value("17")
                        .semanticId(new DefaultReference.Builder()
                                .type(ReferenceTypes.EXTERNAL_REFERENCE)
                                .keys(new DefaultKey.Builder()
                                        .type(KeyTypes.GLOBAL_REFERENCE)
                                        .value("http://my-external-ref.com/ref.xml")
                                        .build())
                                .build())
                        .build(),
                new DefaultSubmodelElementList.Builder().description(new DefaultLangStringTextType.Builder().text("Test Description").build()).build(),
                new DefaultSubmodelElementCollection.Builder().description(new DefaultLangStringTextType.Builder().text("Test Description").build()).build(),
                new DefaultBlob.Builder().contentType("application/xml").build()
        );

        String listIdShort = "test-id-short";
        SubmodelElementList submodelElementList = new DefaultSubmodelElementList.Builder().idShort(listIdShort).value(listElements).build();

        Asset mapped = testSubject.map(parentReference, submodelElementList, mockService);

        assertNotNull(mapped);
        assertNotNull(mapped.getProperties());
        assertNotNull(mapped.getProperty(SMC_CHILDREN_LOCATION));
        assertInstanceOf(List.class, mapped.getProperty(SMC_CHILDREN_LOCATION));
        assertFalse(((List<?>) mapped.getProperty(SMC_CHILDREN_LOCATION)).isEmpty());
        assertInstanceOf(Asset.class, ((List<?>) mapped.getProperty(SMC_CHILDREN_LOCATION)).get(0));

        List<Asset> mappedChildren = (List<Asset>) mapped.getProperty(SMC_CHILDREN_LOCATION);

        assertEquals(listElements.size(), mappedChildren.size());

        // Elements that have no idShort should not get one after mapping
        assertTrue(mappedChildren.stream().allMatch(elem -> elem.getProperty("idShort") == null));

        String listAccessorPathTemplate = "submodels/"
                .concat(Base64.getEncoder().encodeToString(parentSubmodelId.getBytes(StandardCharsets.UTF_8)))
                .concat("/submodel-elements/")
                .concat(listIdShort)
                .concat("[%s]");

        Asset mappedProperty = mappedChildren.get(0);
        assertEquals(AAS_PREFIX.concat(":").concat("Property"), mappedProperty.getProperty(AAS_V30_NAMESPACE.concat("modelingType")));
        assertInstanceOf(AasDataAddress.class, mappedProperty.getDataAddress());
        assertEquals(mockService.baseUrl().toString(), ((AasDataAddress) mappedProperty.getDataAddress()).getBaseUrl());
        assertEquals(String.format(listAccessorPathTemplate, "0"), ((AasDataAddress) mappedProperty.getDataAddress()).getPath());


        Asset mappedList = mappedChildren.get(1);
        assertEquals(AAS_PREFIX.concat(":").concat("SubmodelElementList"), mappedList.getProperty(AAS_V30_NAMESPACE.concat("modelingType")));
        assertInstanceOf(AasDataAddress.class, mappedList.getDataAddress());
        assertEquals(mockService.baseUrl().toString(), ((AasDataAddress) mappedList.getDataAddress()).getBaseUrl());
        assertEquals(String.format(listAccessorPathTemplate, "1"), ((AasDataAddress) mappedList.getDataAddress()).getPath());


        Asset mappedCollection = mappedChildren.get(2);
        assertEquals(AAS_PREFIX.concat(":").concat("SubmodelElementCollection"),
                mappedCollection.getProperty(AAS_V30_NAMESPACE.concat("modelingType")));
        assertInstanceOf(AasDataAddress.class, mappedCollection.getDataAddress());
        assertEquals(mockService.baseUrl().toString(), ((AasDataAddress) mappedCollection.getDataAddress()).getBaseUrl());
        assertEquals(String.format(listAccessorPathTemplate, "2"), ((AasDataAddress) mappedCollection.getDataAddress()).getPath());


        Asset mappedBlob = mappedChildren.get(3);
        assertEquals(AAS_PREFIX.concat(":").concat("Blob"), mappedBlob.getProperty(AAS_V30_NAMESPACE.concat("modelingType")));
        assertInstanceOf(AasDataAddress.class, mappedBlob.getDataAddress());
        assertEquals(mockService.baseUrl().toString(), ((AasDataAddress) mappedBlob.getDataAddress()).getBaseUrl());
        assertEquals(String.format(listAccessorPathTemplate, "3"), ((AasDataAddress) mappedBlob.getDataAddress()).getPath());
    }


    private Service mockService() {
        try {
            return new Service.Builder().withUrl(new URL("https://test-url:1234/api/v3.0")).build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}