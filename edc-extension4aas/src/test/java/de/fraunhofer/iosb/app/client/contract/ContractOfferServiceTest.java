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
package de.fraunhofer.iosb.app.client.contract;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.NotActiveException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContractOfferServiceTest {

    private final int providerPort = 54321;
    private final CatalogService mockCatalogService = mock(CatalogService.class);

    private ContractOfferService contractOfferService;
    private final URL testUrl = new URL("http://localhost:" + providerPort);

    public ContractOfferServiceTest() throws MalformedURLException {
    }

    @BeforeEach
    void initializeContractOfferService() {
        contractOfferService = new ContractOfferService(mockCatalogService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getContractForAssetIdTest() throws InterruptedException, ExecutionException, JsonProcessingException {
        var mockedFuture = mock(CompletableFuture.class);
        var contractOffers = new ArrayList<>(
                List.of(ContractOffer.Builder.newInstance().policy(Policy.Builder.newInstance().build())
                        .assetId("test-asset-id")
                        .id("mocked-contract-id")
                        .build()));
        when(mockedFuture.get())
                .thenReturn(new ObjectMapper().writeValueAsBytes(Catalog.Builder.newInstance().id("catalog-id").contractOffers(contractOffers).build()));

        when(mockCatalogService.request(any(), any(), any())).thenReturn(mockedFuture);

        assertEquals("mocked-contract-id",
                contractOfferService.getContractsForAssetId(testUrl, "test-asset-id").get(0).getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getContractUnreachableProviderTest() throws MalformedURLException, InterruptedException {
        var mockedFuture = mock(CompletableFuture.class);
        when(mockCatalogService.request(any(), any(), any())).thenReturn(mockedFuture);
        mockedFuture.completeExceptionally(new NotActiveException());
        try {
            contractOfferService.getContractsForAssetId(new URL("http://fakeUrl:4321/not/working"), "test-asset-id");
            fail("This should not complete without throwing an exception");
        } catch (EdcException expected) {

        }
    }

}
