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
package de.fraunhofer.iosb.client.policy;

import de.fraunhofer.iosb.client.exception.AmbiguousOrNullException;
import de.fraunhofer.iosb.client.testutils.FileManager;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * We assume here that catalogService does not return null objects as well as null catalogs inside their return value.
 * Also, we assume that catalogs are valid JSON and expandable by the connector's JSON LD expander TitaniumJsonLd.class.
 * Finally, we assume that catalogs can be transformed with the TypeTransformerRegistry.
 */
public class PolicyServiceTest {

    private final URL testUrl;
    private CatalogService catalogService;
    private TypeTransformerRegistry typeTransformerRegistry;
    private PolicyServiceConfig config;
    private PolicyService policyService;

    public PolicyServiceTest() throws MalformedURLException {
        int providerPort = 54321;
        testUrl = new URL("http://localhost:" + providerPort);
    }


    @BeforeEach
    void initializePolicyService() {
        catalogService = mock(CatalogService.class);
        typeTransformerRegistry = mock(TypeTransformerRegistry.class);
        var policyDefinitionStore = mock(PolicyDefinitionStore.class);
        config = mock(PolicyServiceConfig.class);

        policyService = new PolicyService(catalogService, typeTransformerRegistry, config, policyDefinitionStore, mock(Monitor.class));
    }

    @Test
    void getDatasetCatalogResponseFailureTest() throws InterruptedException {
        var querySpec = QuerySpec.Builder.newInstance().filter(List.of(criterion(Asset.PROPERTY_ID, "=", "test-asset-id"))).build();
        var future = new CompletableFuture<StatusResult<byte[]>>();
        future.complete(StatusResult.failure(ResponseStatus.FATAL_ERROR, "This is a test"));

        when(catalogService.requestCatalog("test-counter-party-id", testUrl.toString(), DATASPACE_PROTOCOL_HTTP, querySpec)).thenReturn(future);

        try {
            policyService.getDatasetForAssetId("test-counter-party-id", testUrl, "test-asset-id");
            fail(); // Should throw exception
        } catch (EdcException expected) {
            assertEquals(format("Catalog by provider %s couldn't be retrieved: %s", testUrl, "[This is a test]"), expected.getMessage());
        }
    }

    @Test
    void getDatasetCatalogFutureTimeoutTest() throws InterruptedException {
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion(Asset.PROPERTY_ID, "=", "test-asset-id")).build();
        var future = new CompletableFuture<StatusResult<byte[]>>();

        when(catalogService.requestCatalog("test-counter-party-id", testUrl.toString(), DATASPACE_PROTOCOL_HTTP, querySpec)).thenReturn(future);

        try {
            policyService.getDatasetForAssetId("test-counter-party-id", testUrl, "test-asset-id");
            fail(); // Should throw exception
        } catch (EdcException expected) {
            assertEquals(format("Timeout while waiting for catalog by provider %s.", testUrl), expected.getMessage());
        }
    }

    @Test
    void getDatasetNoDatasetsTest() throws InterruptedException {
        var querySpec = QuerySpec.Builder.newInstance().filter(criterion(Asset.PROPERTY_ID, "=", "test-asset-id")).build();
        var future = new CompletableFuture<StatusResult<byte[]>>();
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        future.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        when(catalogService.requestCatalog("test-counter-party-id", testUrl.toString(), DATASPACE_PROTOCOL_HTTP, querySpec)).thenReturn(future);
        when(typeTransformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(Catalog.Builder.newInstance().build()));

        try {
            policyService.getDatasetForAssetId("test-counter-party-id", testUrl, "test-asset-id");
            fail(); // Should throw exception
        } catch (AmbiguousOrNullException expected) {
            assertEquals(format("Multiple or no policyDefinitions were found for assetId %s!", "test-asset-id"), expected.getMessage());
        }
    }

    @Test
    void getDatasetTest() throws InterruptedException {
        var mockedFuture = new CompletableFuture<StatusResult<byte[]>>();
        var datasetId = "ef4d028f-70d7-404a-b22e-c5b0ffa3aa0b";
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        mockedFuture.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        when(catalogService.requestCatalog(any(), any(), any(), any())).thenReturn(mockedFuture);

        when(typeTransformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(
                        Catalog.Builder.newInstance()
                                .dataset(Dataset.Builder.newInstance()
                                        .id(datasetId)
                                        .offer(UUID.randomUUID().toString(), Policy.Builder.newInstance().build())
                                        .distribution(
                                                Distribution.Builder.newInstance()
                                                        .dataService(DataService.Builder.newInstance().build())
                                                        .format("")
                                                        .build())
                                        .build())
                                .build()));

        assertEquals(datasetId, policyService.getDatasetForAssetId("provider", testUrl, "test-asset-id").getId());
    }

    @Test
    void getAcceptablePolicyForAssetIdTest() throws InterruptedException {
        var shouldPolicy = Policy.Builder.newInstance().build();
        var dataset = Dataset.Builder.newInstance()
                .offer("test-offer-id", shouldPolicy).build();
        // mock getDatasetMethod
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(dataset)
                .when(policyServiceSpy)
                .getDatasetForAssetId("test-counter-party-id", testUrl, "test-asset-id");
        when(config.isAcceptAllProviderOffers()).thenReturn(true);

        var resultPolicy = policyServiceSpy.getAcceptablePolicyForAssetId("test-counter-party-id", testUrl, "test-asset-id");
        assertEquals(shouldPolicy, resultPolicy.getSecond());
        assertEquals("test-offer-id", resultPolicy.getFirst());
    }

    @Test
    void getAcceptablePolicyForAssetIdEmptyPolicyListTest() throws InterruptedException {
        // mock getDatasetMethod
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(Dataset.Builder.newInstance().build()).when(policyServiceSpy).getDatasetForAssetId("test-counter-party-id", testUrl, "test-asset-id");

        try {
            policyServiceSpy.getAcceptablePolicyForAssetId("test-counter-party-id", testUrl, "test-asset-id");
        } catch (EdcException expected) {
            assertEquals("Could not find any acceptable policyDefinition", expected.getMessage());
        }

    }

    @Test
    void getAcceptablePolicyForAssetIdAcceptAllOffersTest() {
        //TODO
    }

    @Test
    void getAcceptablePolicyForAssetIdAcceptFromAcceptedListTest() {
        //TODO
    }

    @Test
    void getAcceptablePolicyForAssetIdNoAcceptablePolicyTest() {
        //TODO
    }

    @Test
    void getAcceptablePolicyForAssetIdTimeoutTest() {
        //TODO
    }

    @Test
    void getAcceptablePolicyForAssetIdExceptionbyGetDatasetTest() {
        //TODO
    }
}
