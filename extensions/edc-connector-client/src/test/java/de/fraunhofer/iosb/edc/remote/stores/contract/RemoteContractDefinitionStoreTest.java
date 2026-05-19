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
package de.fraunhofer.iosb.edc.remote.stores.contract;

import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.edc.remote.stores.AbstractControlPlaneConnectionHandlerTest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class RemoteContractDefinitionStoreTest extends AbstractControlPlaneConnectionHandlerTest {

    @Test
    void remoteAssetIndex_queryContractDefinitionsFoundAndReturned() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/contractdefinitions/request");

        List<ContractDefinition> contractDefinitions = List.of(getContractDefinition(), getContractDefinition());
        when(mockCodec.deserializeList("test-return-body", ContractDefinition.class))
                .thenReturn(Result.success(contractDefinitions));

        var response = testSubject.findAll(querySpec);

        assertEquals(contractDefinitions, response.toList());
    }


    @Test
    void remoteAssetIndex_queryContractDefinitionsEmptyResponse() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/contractdefinitions/request");

        List<ContractDefinition> contractDefinitions = List.of();
        when(mockCodec.deserializeList("test-return-body", ContractDefinition.class))
                .thenReturn(Result.success(contractDefinitions));

        var response = testSubject.findAll(querySpec);

        assertEquals(contractDefinitions, response.toList());
    }


    @Test
    void findAssetById_assetFoundAndReturned() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        var contractDefinition = getContractDefinition();
        when(mockCodec.deserialize("test-return-body", ContractDefinition.class))
                .thenReturn(Result.success(contractDefinition));

        mockResponseForGet(String.format("/contractdefinitions/%s", id));

        var response = testSubject.findById(id);

        assertEquals(contractDefinition, response);
    }


    @Test
    void findAssetById_notFound() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        // Returns 404 for the id request
        //mockResponseForGet(String.format("/contractdefinitions/%s", id));

        var response = testSubject.findById(id);

        assertNull(response);
    }


    @Test
    void connectionHandler_authorizes() {
        authorizedServer();

        var testSubject = testSubject();

        when(mockCodec.serialize(any())).thenReturn("test");

        var response = testSubject.findAll(QuerySpec.max());
        assertNotNull(response);
    }


    @Test
    void connectionHandler_wrongPasswordNoFailure_andLogs() {
        authorizedServer();

        var testSubject = new RemoteContractDefinitionStore.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey.concat("prefixMakingApiKeyFalse"), vault))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .vault(vault)
                .build();

        when(mockCodec.serialize(any())).thenReturn(
                "{" +
                        "  \"@context\": {" +
                        "    \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\"" +
                        "  }," +
                        "  \"@type\": \"QuerySpec\"" +
                        "}");

        Stream<ContractDefinition> response = testSubject.findAll(QuerySpec.max());
        assertNotNull(response);
        assertTrue(response.findAny().isEmpty());
    }


    private RemoteContractDefinitionStore testSubject() {
        return new RemoteContractDefinitionStore.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey, vault))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .vault(vault)
                .build();
    }


    private ContractDefinition getContractDefinition() {
        return ContractDefinition.Builder.newInstance()
                .accessPolicyId(UUID.randomUUID().toString())
                .contractPolicyId(UUID.randomUUID().toString())
                .build();
    }
}
