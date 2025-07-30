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
package de.fraunhofer.iosb.ilt.client.policy;

import de.fraunhofer.iosb.ilt.client.testutils.FileManager;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Catalog;
import org.eclipse.edc.connector.controlplane.catalog.spi.DataService;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.catalog.spi.Distribution;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.query.Criterion.criterion;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.UNEXPECTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    private static final String TEST_COUNTER_PARTY_ID = "test-counter-party-id";
    private static final String TEST_ASSET_ID = "test-asset-id";
    private static final QuerySpec ASSET_ID_QUERY_SPEC = QuerySpec.Builder.newInstance()
            .filter(List.of(
                    criterion(Asset.PROPERTY_ID, "=", TEST_ASSET_ID)))
            .build();

    private static CompletableFuture<StatusResult<byte[]>> future;

    private final URL testUrl;
    private CatalogService catalogService;
    private TypeTransformerRegistry typeTransformerRegistry;
    private PolicyServiceConfig config;
    private PolicyService policyService;
    private PolicyDefinitionStore policyDefinitionStore;

    public PolicyServiceTest() throws MalformedURLException {
        int providerPort = 54321;
        testUrl = new URL("http://localhost:" + providerPort);
    }

    @BeforeEach
    void initializePolicyService() {
        catalogService = mock(CatalogService.class);
        typeTransformerRegistry = mock(TypeTransformerRegistry.class);
        config = mock(PolicyServiceConfig.class);
        policyDefinitionStore = mock(PolicyDefinitionStore.class);

        policyService = new PolicyService(catalogService, typeTransformerRegistry, config, policyDefinitionStore, new ConsoleMonitor().withPrefix(
                "PolicyServiceTest"));

        future = new CompletableFuture<>();
    }

    @Test
    void getDatasetCatalogResponseFailureTest() {
        future.complete(StatusResult.failure(ResponseStatus.FATAL_ERROR, "This is a test"));

        when(catalogService.requestCatalog(TEST_COUNTER_PARTY_ID, testUrl.toString(), DATASPACE_PROTOCOL_HTTP, ASSET_ID_QUERY_SPEC)).thenReturn(future);

        var response = policyService.getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);
        assertEquals("Failed fetching catalog, FATAL_ERROR: This is a test", response.getFailureDetail());

    }

    @Test
    @SuppressWarnings("unchecked")
    void getDatasetCatalogFutureTimeoutTest() throws ExecutionException, InterruptedException, TimeoutException {
        final int timeout = 20;
        final String timeoutMessage = "Request timed out";
        when(config.getWaitForCatalogTimeout()).thenReturn(timeout);
        CompletableFuture<StatusResult<byte[]>> future2 = mock(CompletableFuture.class);
        when(future2.get(timeout, TimeUnit.SECONDS)).thenThrow(new TimeoutException(timeoutMessage));

        mockCatalogServiceResponseWith(future);

        var datasetResponse = policyService.getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(datasetResponse.failed());
        assertEquals(UNEXPECTED, datasetResponse.reason());
        //assertTrue(datasetResponse.getFailureMessages().contains(TIMEOUT_MESSAGE));
    }

    @Test
    void getDatasetNoDatasetsTest() {
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        future.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        mockCatalogServiceResponseWith(future);

        when(typeTransformerRegistry.transform(any(), any())).thenReturn(Result.success(Catalog.Builder.newInstance().build()));

        var datasetResponse = policyService.getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(datasetResponse.failed());
        assertEquals(ServiceFailure.Reason.NOT_FOUND, datasetResponse.reason());
    }

    @Test
    void getDatasetTooManyDatasetsTest() {
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        future.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        mockCatalogServiceResponseWith(future);

        when(typeTransformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(
                        Catalog.Builder.newInstance()
                                .datasets(List.of(Dataset.Builder.newInstance().build(),
                                        Dataset.Builder.newInstance().build()))
                                .build()));

        var datasetResponse = policyService.getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(datasetResponse.failed());
        assertEquals(ServiceFailure.Reason.CONFLICT, datasetResponse.reason());
    }

    @Test
    void getDatasetTest() {
        var dataset = getDataset();
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        future.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        mockCatalogServiceResponseWith(future);

        when(typeTransformerRegistry.transform(any(), any()))
                .thenReturn(Result.success(
                        Catalog.Builder.newInstance()
                                .dataset(dataset)
                                .build()));

        assertEquals(dataset.getId(), policyService.getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID).getContent().getId());
    }

    @Test
    void getAcceptableContractOfferForAssetIdTest() {
        var dataset = getDataset();

        var datasetResult = ServiceResult.success(dataset);

        // mock getDatasetForAssetId
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(datasetResult)
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);
        when(config.isAcceptAllProviderOffers()).thenReturn(true);

        var resultPolicy = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertEquals(dataset.getOffers().values().stream().findFirst().orElseThrow(),
                resultPolicy.getContent().getPolicy());
        assertEquals(dataset.getOffers().keySet().stream().findFirst().orElseThrow(),
                resultPolicy.getContent().getId());
    }

    @Test
    void getAcceptablePolicyForAssetIdEmptyContractOfferListTest() {
        // we mock getDatasetMethod for simplicity
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(ServiceResult.success(Dataset.Builder.newInstance().build()))
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        var result = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(result.failed());
    }

    @Test
    void test_getAcceptableContractOfferForAssetId_acceptAllOffers() {
        when(config.isAcceptAllProviderOffers()).thenReturn(true);

        Dataset dataset = getDataset();

        // we mock getDatasetMethod for simplicity
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(ServiceResult.success(dataset))
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        var result = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(result.succeeded());

        // Compare contractoffer id and policy
        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getValue(), result.getContent().getPolicy());

        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getValue().getTarget(), result.getContent().getAssetId());

        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getKey(), result.getContent().getId());
    }

    @Test
    void test_getAcceptableContractOfferForAssetId_acceptFromAcceptedList() {
        var dataset = getDataset();
        when(config.isAcceptAllProviderOffers()).thenReturn(false);
        List<PolicyDefinition> policies = List.of(
                PolicyDefinition.Builder.newInstance()
                        .policy(dataset.getOffers().values().stream().findFirst().orElseThrow())
                        .id(dataset.getOffers().keySet().stream().findFirst().orElseThrow())
                        .build());

        when(policyDefinitionStore.getPolicyDefinitions()).thenReturn(policies);

        // we mock getDatasetMethod for simplicity
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(ServiceResult.success(dataset))
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        var result = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(result.succeeded());

        // Compare contract offer id and policy
        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getValue(), result.getContent().getPolicy());

        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getValue().getTarget(), result.getContent().getAssetId());

        assertEquals(dataset.getOffers().entrySet().stream().findFirst().orElseThrow().getKey(), result.getContent().getId());
    }

    @Test
    void test_getAcceptableContractOfferForAssetId_noAcceptableContractOffer() {
        var dataset = getDataset();
        when(config.isAcceptAllProviderOffers()).thenReturn(false);
        List<PolicyDefinition> policies = List.of(
                PolicyDefinition.Builder.newInstance()
                        .policy(Policy.Builder.newInstance()
                                .permission(Permission.Builder.newInstance().build())
                                .build())
                        .id(dataset.getOffers().keySet().stream().findFirst().orElseThrow())
                        .build());

        when(policyDefinitionStore.getPolicyDefinitions()).thenReturn(policies);

        // we mock getDatasetMethod for simplicity
        var policyServiceSpy = spy(policyService);
        Mockito.doReturn(ServiceResult.success(dataset))
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        var result = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(result.failed());
    }

    @Test
    void test_getAcceptableContractOfferForAssetId_timeoutErrorHandling() {
        final String errorMessage = "Failed fetching catalog";
        // we mock getDatasetMethod for simplicity
        var policyServiceSpy = spy(policyService);

        Mockito.doReturn(ServiceResult.unexpected(errorMessage))
                .when(policyServiceSpy)
                .getDatasetForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        var result = policyServiceSpy.getAcceptableContractOfferForAssetId(TEST_COUNTER_PARTY_ID, testUrl, TEST_ASSET_ID);

        assertTrue(result.failed());

        assertTrue(result.getFailureMessages().contains(UNEXPECTED.toString()));
        assertTrue(result.getFailureMessages().contains(errorMessage));
    }

    private Dataset getDataset() {
        return Dataset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .offer(UUID.randomUUID().toString(),
                        Policy.Builder.newInstance()
                                .target(TEST_ASSET_ID)
                                .duty(Duty.Builder.newInstance()
                                        .action(Action.Builder.newInstance().type("USE").build())
                                        .consequence(Duty.Builder.newInstance().build()).build()).build())
                .distribution(Distribution.Builder.newInstance()
                        .dataService(DataService.Builder.newInstance().build())
                        .format("")
                        .build())
                .build();
    }

    private void mockCatalogServiceResponseWith(CompletableFuture<StatusResult<byte[]>> value) {
        when(catalogService.requestCatalog(
                TEST_COUNTER_PARTY_ID,
                testUrl.toString(),
                DATASPACE_PROTOCOL_HTTP,
                ASSET_ID_QUERY_SPEC))
                .thenReturn(value);
    }
}
