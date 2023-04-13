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
package de.fraunhofer.iosb.app.client;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferObservable;
import jakarta.ws.rs.core.Response;

public class ClientEndpointTest {

    private static int port;
    private static URL url;
    private static ClientAndServer mockServer;

    private ClientEndpoint clientEndpoint;

    private static ContractOffer mockContractOffer;
    private static Catalog mockCatalog;

    @BeforeAll
    public static void initialize() throws IOException {
        Logger.getInstance().setMonitor(mock(Monitor.class));
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
        mockServer = startClientAndServer(port);
        var mockAsset = Asset.Builder.newInstance().id("test-asset").build();
        var mockPolicy = Policy.Builder.newInstance().build();
        var mockedContractStart = ZonedDateTime.now();
        var mockedContractEnd = ZonedDateTime.of(mockedContractStart.toLocalDate(),
                mockedContractStart.toLocalTime().plusHours(1), mockedContractStart.getZone());
        mockContractOffer = ContractOffer.Builder.newInstance()
                .id("test-contract-offer")
                .asset(mockAsset)
                .policy(mockPolicy)
                .contractStart(mockedContractStart)
                .contractEnd(mockedContractEnd)
                .provider(URI.create(url.toString()))
                .build();
        var contractOffers = new ArrayList<ContractOffer>();
        contractOffers.add(mockContractOffer);

        mockCatalog = Catalog.Builder.newInstance().id("test-catalog").contractOffers(contractOffers).build();
    }

    @BeforeEach
    public void setupSynchronizer() {
        clientEndpoint = new ClientEndpoint(URI.create("http://localhost:8181/api"),
                mockCatalogService(), mockConsumerNegotiationManager(),
                mock(ContractNegotiationStore.class),
                mock(ContractNegotiationObservable.class), mockTransferProcessManager(),
                mock(DataTransferObservable.class), mock(CustomAuthenticationRequestFilter.class));
    }

    private TransferProcessManager mockTransferProcessManager() {
        StatusResult<String> mockStatusResult = StatusResult.failure(ResponseStatus.FATAL_ERROR);

        var mockTransferProcessManager = mock(TransferProcessManager.class);
        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
        return mockTransferProcessManager;
    }

    private CatalogService mockCatalogService() {
        var catalogService = mock(CatalogService.class);
        var completableFuture = new CompletableFuture<Catalog>();
        completableFuture.complete(mockCatalog);

        when(catalogService.getByProviderUrl(any(), any())).thenReturn(completableFuture);
        return catalogService;
    }

    private ConsumerContractNegotiationManager mockConsumerNegotiationManager() {
        var mockStatusResult = StatusResult.success(
                ContractNegotiation.Builder.newInstance()
                        .id("test-ContractNegotiation-id")
                        .counterPartyId("test-ContractNegotiation-counterparty-id")
                        .counterPartyAddress("test-ContractNegotiation-counterparty-address")
                        .protocol("test-ContractNegotiation-protocol")
                        .build());
        var manager = mock(ConsumerContractNegotiationManager.class);
        when(manager.initiate(any())).thenReturn(mockStatusResult);
        return manager;
    }

    @AfterEach
    public void shutdownMockServer() {
        if (Objects.nonNull(mockServer) && mockServer.isRunning()) {
            mockServer.stop();
        }
    }

    @Test
    public void negotiateContractTest() {
        try {
            clientEndpoint.negotiateContract(url, mockContractOffer);
            fail();
        } catch (EdcException expected) {
            if (!(expected.getCause().getClass().equals(TimeoutException.class)
                    && expected.getMessage().contains("agreement"))) {
                fail(); // This must fail because of agreement timeout.
            }
        }
    }

    @Test
    public void negotiateContractAndTransferTest() {
        try {
            clientEndpoint.negotiateContract(url, "test-asset", null);
            fail();
        } catch (EdcException expected) {
        }
    }

    @Test
    public void getDataTest() {
        try {
            clientEndpoint.getData(url, "test-agreement-id", "test-asset-id", url);
            fail();
        } catch (EdcException expected) {
        }
    }

    @Test
    public void getContractOffersTest() {
        var responseEntity = clientEndpoint.getContractOffers(url, "test-asset").getEntity().toString();
        assertEquals(format("[%s]", mockContractOffer.toString()), responseEntity);
    }

    @Test
    public void getAcceptedContractOffersTest() {
        assertEquals(Response.Status.OK.getStatusCode(), clientEndpoint.getAcceptedContractOffers().getStatus());
    }

    @Test
    public void addAcceptedContractOffersTest() {
        var mockContractOfferAsList = new ArrayList<ContractOffer>();
        mockContractOfferAsList.add(mockContractOffer); // ClientEndpoint creates ArrayList
        var offers = new ContractOffer[] { mockContractOffer };

        clientEndpoint.addAcceptedContractOffers(offers);

        assertEquals(mockContractOfferAsList, clientEndpoint.getAcceptedContractOffers().getEntity());
    }

    @Test
    public void updateAcceptedContractOfferTest() {
        var offers = new ContractOffer[] { mockContractOffer };

        clientEndpoint.addAcceptedContractOffers(offers);

        var mockAsset = Asset.Builder.newInstance().id("test-asset2").build();
        var mockPolicy = Policy.Builder.newInstance().build();
        var mockedContractStart = ZonedDateTime.now();
        var mockedContractEnd = ZonedDateTime.of(mockedContractStart.toLocalDate(),
                mockedContractStart.toLocalTime().plusHours(1), mockedContractStart.getZone());
        var mockUpdatedContractOffer = ContractOffer.Builder.newInstance()
                .id("test-contract-offer") // Same id <-> same offer
                .asset(mockAsset)
                .policy(mockPolicy)
                .contractStart(mockedContractStart)
                .contractEnd(mockedContractEnd)
                .provider(URI.create(url.toString()))
                .build();

        var mockContractOfferAsList = new ArrayList<ContractOffer>();
        mockContractOfferAsList.add(mockUpdatedContractOffer); // ClientEndpoint creates ArrayList
        clientEndpoint.updateAcceptedContractOffer(mockUpdatedContractOffer);

        assertEquals(mockContractOfferAsList, clientEndpoint.getAcceptedContractOffers().getEntity());

    }
}
