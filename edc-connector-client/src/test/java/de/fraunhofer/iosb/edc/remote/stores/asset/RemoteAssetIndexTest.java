package de.fraunhofer.iosb.edc.remote.stores.asset;

import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.edc.remote.ControlPlaneConnectionException;
import de.fraunhofer.iosb.edc.remote.stores.AbstractControlPlaneConnectionHandlerTest;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class RemoteAssetIndexTest extends AbstractControlPlaneConnectionHandlerTest {

    @Test
    void remoteAssetIndex_queryAssetsFoundAndReturned() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/assets/request");

        List<Asset> assets = List.of(getAsset(), getAsset());
        when(mockCodec.deserializeList("test-return-body", Asset.class)).thenReturn(Result.success(assets));

        var response = testSubject.queryAssets(querySpec);

        assertEquals(assets, response.toList());
    }

    @Test
    void remoteAssetIndex_queryAssetsEmptyResponse() {
        var querySpec = QuerySpec.none();
        var testSubject = testSubject();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/assets/request");

        List<Asset> assets = List.of();
        when(mockCodec.deserializeList("test-return-body", Asset.class)).thenReturn(Result.success(assets));

        var response = testSubject.queryAssets(querySpec);

        assertEquals(assets, response.toList());
    }

    @Test
    void findAssetById_assetFoundAndReturned() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        var asset = getAsset();
        when(mockCodec.deserialize("test-return-body", Asset.class)).thenReturn(Result.success(asset));

        mockResponseForGet(String.format("/assets/%s", id));

        var response = testSubject.findById(id);

        assertEquals(asset, response);
    }

    @Test
    void findAssetById_assetNotFound() {
        var id = UUID.randomUUID().toString();
        var testSubject = testSubject();

        // Returns 404 for the id request
        //mockResponseForGet(String.format("/assets/%s", id));

        var response = testSubject.findById(id);

        assertNull(response);
    }

    @Test
    void connectionHandler_authorizes() {
        authorizedServer();

        var testSubject = testSubject();

        when(mockCodec.serialize(any())).thenReturn("{" +
                "  \"@context\": {" +
                "    \"@vocab\": \"https://w3id.org/edc/v0.0.1/ns/\"" +
                "  }," +
                "  \"@type\": \"QuerySpec\"" +
                "}");

        var response = testSubject.queryAssets(QuerySpec.max());
        assertNotNull(response);
    }

    @Test
    void connectionHandler_wrongPasswordNoFailure_andLogs() {
        authorizedServer();

        var testSubject = new RemoteAssetIndex.Builder()
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
        try {
            testSubject.queryAssets(QuerySpec.max());
            fail();
        } catch (ControlPlaneConnectionException expected) {
        }
    }

    private RemoteAssetIndex testSubject() {
        return new RemoteAssetIndex.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey))
                .managementUri(String.format("http://localhost:%s", server.getPort()))
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();
    }

    private Asset getAsset() {
        return Asset.Builder.newInstance().property("aas:id", UUID.randomUUID().toString()).build();
    }
}