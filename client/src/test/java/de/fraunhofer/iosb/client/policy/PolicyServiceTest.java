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

import de.fraunhofer.iosb.client.testUtils.FileManager;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.DataService;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.catalog.spi.Distribution;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PolicyServiceTest {

    private final int providerPort = 54321;
    private final CatalogService mockCatalogService = mock(CatalogService.class);
    private final TypeTransformerRegistry mockTransformer = mock(TypeTransformerRegistry.class);

    private PolicyService policyService;
    private final URL testUrl = new URL("http://localhost:" + providerPort);

    public PolicyServiceTest() throws MalformedURLException {
    }

    @BeforeEach
    void initializeContractOfferService() {
        policyService = new PolicyService(mockCatalogService, mockTransformer, mockConfig(),
                mock(PolicyDefinitionStore.class), mock(Monitor.class));
    }

    @Test
    void getPolicyForAssetIdTest() throws InterruptedException {
        var mockedFuture = new CompletableFuture<StatusResult<byte[]>>();
        var datasetId = "ef4d028f-70d7-404a-b22e-c5b0ffa3aa0b";
        var catalogString = FileManager.loadResource("catalog.json");
        assert catalogString != null;
        mockedFuture.complete(StatusResult.success(catalogString.getBytes(StandardCharsets.UTF_8)));

        when(mockCatalogService.requestCatalog(any(), any(), any(), any())).thenReturn(mockedFuture);

        when(mockTransformer.transform(any(), any())).thenReturn(Result.success(Catalog.Builder.newInstance()
                .dataset(Dataset.Builder.newInstance()
                        .id(datasetId)
                        .offer(UUID.randomUUID().toString(),
                                Policy.Builder.newInstance().build())
                        .distribution(Distribution.Builder.newInstance()
                                .dataService(DataService.Builder.newInstance().build())
                                .format("")
                                .build())
                        .build())
                .build()));

        assertEquals(datasetId, policyService.getDatasetForAssetId("provider", testUrl, "test-asset-id").getId());
    }

    @Test
    void getContractUnreachableProviderTest() throws MalformedURLException, InterruptedException {
        var mockedFuture = new CompletableFuture<StatusResult<byte[]>>();
        when(mockCatalogService.requestCatalog(any(), any(), any(), any())).thenReturn(mockedFuture);

        try {
            policyService.getDatasetForAssetId("provider", new URL("http://fakeUrl:4321/not/working"), "test-asset-id");
            fail("This should not complete without throwing an exception");
        } catch (EdcException expected) {
        }
    }

    private PolicyServiceConfig mockConfig() {
        return new PolicyServiceConfig(ConfigFactory.fromMap(Map.of(
                "edc.dsp.callback.address", "http://localhost:4321/dsp",
                "web.http.port", "8080",
                "web.http.path", "/api")));

    }
}
