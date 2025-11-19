package de.fraunhofer.iosb.edc.remote.stores.asset;

import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import de.fraunhofer.iosb.edc.remote.stores.AbstractControlPlaneConnectionHandlerTest;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


class RemoteAssetIndexTest extends AbstractControlPlaneConnectionHandlerTest {

    @Test
    void queryAssets_foundAndReturned() throws MalformedURLException {
        var querySpec = QuerySpec.none();
        var testSubject = getRemoteAssetIndex();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/assets/request");

        List<Asset> assets = List.of(getAsset(), getAsset());
        when(mockCodec.deserializeList("test-return-body", Asset.class)).thenReturn(Result.success(assets));

        var response = testSubject.queryAssets(querySpec);

        assertEquals(assets, response.toList());
    }


    @Test
    void queryAssets_emptyResponse() {
        var querySpec = QuerySpec.none();
        var testSubject = getRemoteAssetIndex();

        when(mockCodec.serialize(querySpec)).thenReturn("test-body");

        mockResponseForPost("/assets/request");

        List<Asset> assets = List.of();
        when(mockCodec.deserializeList("test-return-body", Asset.class)).thenReturn(Result.success(assets));

        var response = testSubject.queryAssets(querySpec);

        assertEquals(assets, response.toList());
    }


    @Test
    void findById_foundAndReturned() throws MalformedURLException {
        var id = UUID.randomUUID().toString();
        var testSubject = getRemoteAssetIndex();

        var asset = getAsset();
        when(mockCodec.deserialize("test-return-body", Asset.class)).thenReturn(Result.success(asset));

        mockResponseForGet(String.format("/assets/%s", id));

        var response = testSubject.findById(id);

        assertEquals(asset, response);
    }


    @Test
    void findById_assetNotFound() {
        var id = UUID.randomUUID().toString();
        var testSubject = getRemoteAssetIndex();

        // Returns 404 for the id request
        //mockResponseForGet(String.format("/assets/%s", id));

        var response = testSubject.findById(id);

        assertNull(response);
    }


    @Test
    void connectionHandler_authorizes() {
        authorizedServer();

        when(mockCodec.deserializeList(any(), any())).thenReturn(Result.success(List.of()));

        var testSubject = getRemoteAssetIndex();

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

        Stream<Asset> response = testSubject.queryAssets(QuerySpec.max());
        assertNotNull(response);
        assertTrue(response.findAny().isEmpty());
    }


    private RemoteAssetIndex getRemoteAssetIndex() {
        return new RemoteAssetIndex.Builder()
                .authenticationMethod(new ApiKey("x-api-key", apiKey))
                .managementUri(server.baseUrl())
                .codec(mockCodec)
                .httpClient(httpClient)
                .monitor(monitor)
                .build();
    }


    private Asset getAsset() throws MalformedURLException {
        return Asset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .contentType(UUID.randomUUID().toString())
                .version(UUID.randomUUID().toString())
                .description(UUID.randomUUID().toString())
                .property("aas:id", UUID.randomUUID().toString())
                .dataAddress(
                        AasDataAddress.Builder.newInstance()
                                .baseUrl("http://example.com")
                                .additionalHeaders(Map.of(UUID.randomUUID().toString(), UUID.randomUUID().toString()))
                                .method(UUID.randomUUID().toString())
                                .proxyMethod(UUID.randomUUID().toString())
                                .path(UUID.randomUUID().toString())
                                .proxyPath(UUID.randomUUID().toString())
                                .proxyBody(UUID.randomUUID().toString())
                                .proxyOperation(UUID.randomUUID().toString())
                                .build()
                )
                .property("aas:Referable/idShort", UUID.randomUUID().toString())
                .build();
    }
}
