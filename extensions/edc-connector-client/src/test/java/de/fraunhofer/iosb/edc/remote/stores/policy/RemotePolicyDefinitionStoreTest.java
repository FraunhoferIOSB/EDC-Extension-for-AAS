package de.fraunhofer.iosb.edc.remote.stores.policy;

import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.edc.remote.stores.AbstractControlPlaneConnectionHandlerTest;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class RemotePolicyDefinitionStoreTest extends AbstractControlPlaneConnectionHandlerTest {

    @Test
    void remoteAssetIndex_queryContractDefinitionsFoundAndReturned() {
        var querySpec = QuerySpec.none();
        var testSubject = remotePolicyDefinitionStore();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/policydefinitions/request");

        List<PolicyDefinition> policyDefinitions = List.of(getPolicyDefinition(), getPolicyDefinition());
        when(mockCodec.deserializeList("test-return-body", PolicyDefinition.class)).thenReturn(Result.success(policyDefinitions));

        var response = testSubject.findAll(querySpec);

        assertEquals(policyDefinitions, response.toList());
    }


    @Test
    void remoteAssetIndex_queryContractDefinitionsEmptyResponse() {
        var querySpec = QuerySpec.none();
        var testSubject = remotePolicyDefinitionStore();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/policydefinitions/request");

        List<PolicyDefinition> policyDefinitions = List.of();
        when(mockCodec.deserializeList("test-return-body", PolicyDefinition.class))
                .thenReturn(Result.success(policyDefinitions));

        var response = testSubject.findAll(querySpec);

        assertEquals(policyDefinitions, response.toList());
    }


    @Test
    void findAssetById_assetFoundAndReturned() {
        var id = UUID.randomUUID().toString();
        var testSubject = remotePolicyDefinitionStore();

        var policyDefinition = getPolicyDefinition();
        when(mockCodec.deserialize("test-return-body", PolicyDefinition.class))
                .thenReturn(Result.success(policyDefinition));

        mockResponseForGet(String.format("/policydefinitions/%s", id));

        PolicyDefinition response = testSubject.findById(id);

        assertEquals(policyDefinition, response);
    }


    @Test
    void findAssetById_notFound() {
        var id = UUID.randomUUID().toString();
        var testSubject = remotePolicyDefinitionStore();

        // Returns 404 for the id request
        //mockResponseForGet(String.format("/policydefinitions/%s", id));

        var response = testSubject.findById(id);

        assertNull(response);
    }


    @Test
    void findAssetById_unauthorized() {
        var id = UUID.randomUUID().toString();
        var testSubject = remotePolicyDefinitionStoreWrongAuth();

        // Returns 404 for the id request
        authorizedServer();

        var response = testSubject.findById(id);

        assertNull(response);

        verify(monitor).severe(contains(UNAUTHORIZED.name()));
    }


    @Test
    void connectionHandler_authorizes() {
        authorizedServer();

        var testSubject = remotePolicyDefinitionStore();

        when(mockCodec.serialize(any())).thenReturn("test");

        var response = testSubject.findAll(QuerySpec.max());
        assertNotNull(response);
    }


    @Test
    void connectionHandler_wrongPasswordNoFailure_andLogs() {
        authorizedServer();

        var testSubject = new RemotePolicyDefinitionStore.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey.concat("prefixMakingApiKeyFalse")))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();

        when(mockCodec.serialize(any())).thenReturn(
                "{" +
                        "  \"@context\": {" +
                        "    \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\"" +
                        "  }," +
                        "  \"@type\": \"QuerySpec\"" +
                        "}");

        Stream<PolicyDefinition> response = testSubject.findAll(QuerySpec.max());
        assertNotNull(response);
        assertTrue(response.findAny().isEmpty());

    }


    private RemotePolicyDefinitionStore remotePolicyDefinitionStore() {
        return new RemotePolicyDefinitionStore.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();
    }


    private RemotePolicyDefinitionStore remotePolicyDefinitionStoreWrongAuth() {
        return new RemotePolicyDefinitionStore.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey.concat("wrong")))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();
    }


    private PolicyDefinition getPolicyDefinition() {
        return PolicyDefinition.Builder.newInstance()
                .policy(Policy.Builder.newInstance()
                        .target(UUID.randomUUID().toString())
                        .build())
                .build();
    }
}
