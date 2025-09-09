package de.fraunhofer.iosb.edc.remote.stores.policy;

import de.fraunhofer.iosb.edc.remote.stores.AbstractControlPlaneConnectionHandlerTest;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemotePolicyDefinitionStoreTest extends AbstractControlPlaneConnectionHandlerTest {

    @Test
    void remoteAssetIndex_queryContractDefinitionsFoundAndReturned() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serializeQuerySpec(querySpec)).thenReturn("test-body");

        mockResponseForPost("/policydefinitions/request");

        List<PolicyDefinition> policyDefinitions = List.of(getPolicyDefinition(), getPolicyDefinition());
        when(mockCodec.deserializePolicyDefinitions("test-return-body")).thenReturn(policyDefinitions);

        var response = testSubject.findAll(querySpec);

        assertEquals(policyDefinitions, response.toList());
    }

    @Test
    void remoteAssetIndex_queryContractDefinitionsEmptyResponse() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serializeQuerySpec(querySpec)).thenReturn("test-body");

        mockResponseForPost("/policydefinitions/request");

        List<PolicyDefinition> policyDefinitions = List.of();
        when(mockCodec.deserializePolicyDefinitions("test-return-body"))
                .thenReturn(policyDefinitions);

        var response = testSubject.findAll(querySpec);

        assertEquals(policyDefinitions, response.toList());
    }

    @Test
    void findAssetById_assetFoundAndReturned() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        var policyDefinition = getPolicyDefinition();
        when(mockCodec.deserializePolicyDefinition("test-return-body"))
                .thenReturn(policyDefinition);

        mockResponseForGet(String.format("/policydefinitions/%s", id));

        var response = testSubject.findById(id);

        assertEquals(policyDefinition, response);
    }

    @Test
    void findAssetById_notFound() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        // Returns 404 for the id request
        //mockResponseForGet(String.format("/policydefinitions/%s", id));

        var response = testSubject.findById(id);

        verify(monitor).debug(contains(ServiceFailure.Reason.NOT_FOUND.toString()));

        assertNull(response);
    }

    @Test
    void connectionHandler_authorizes() {
        authorizedServer();

        var testSubject = testSubject();

        when(mockCodec.serializeQuerySpec(any())).thenReturn("test");

        var response = testSubject.findAll(QuerySpec.max());
        assertNotNull(response);
    }

    @Test
    void connectionHandler_wrongPasswordNoFailure_andLogs() {
        authorizedServer();

        var testSubject = new RemotePolicyDefinitionStore.Builder()
                .apiKey(apiKey.concat("prefixMakingApiKeyFalse"))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();

        when(mockCodec.serializeQuerySpec(any())).thenReturn(
                "{" +
                        "  \"@context\": {" +
                        "    \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\"" +
                        "  }," +
                        "  \"@type\": \"QuerySpec\"" +
                        "}");

        var response = testSubject.findAll(QuerySpec.max());

        verify(monitor).warning(contains(ServiceFailure.Reason.UNAUTHORIZED.toString()));
        assertNotNull(response);
    }

    private RemotePolicyDefinitionStore testSubject() {
        return new RemotePolicyDefinitionStore.Builder()
                .apiKey(apiKey)
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