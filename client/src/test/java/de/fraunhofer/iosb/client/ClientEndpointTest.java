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
package de.fraunhofer.iosb.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.client.datatransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
import jakarta.ws.rs.core.Response;

import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.catalog.spi.Dataset;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

public class ClientEndpointTest {

    private static Monitor monitor;

    private static URL url;
    private static ClientAndServer mockServer;

    private ClientEndpoint clientEndpoint;

    private static PolicyDefinition mockPolicyDefinition;
    private static Catalog mockCatalog;

    @BeforeAll
    public static void initialize() throws IOException {
        monitor = mock(Monitor.class);
        int port = 8080;
        url = new URL(format("http://localhost:%s", port));
        mockServer = startClientAndServer(port);
        var mockAsset = Asset.Builder.newInstance().id("test-asset").build();
        var mockPolicy = Policy.Builder.newInstance().target(mockAsset.getId()).build();
        var dataset = Dataset.Builder.newInstance()
                .id(UUID.randomUUID().toString())
                .distribution(null)
                .offer(UUID.randomUUID().toString(), mockPolicy).build();

        mockPolicyDefinition = PolicyDefinition.Builder.newInstance()
                .policy(mockPolicy).build();

        mockCatalog = Catalog.Builder.newInstance().id("test-catalog").dataset(dataset).build();
    }

    @BeforeEach
    public void setup() throws IOException {
        clientEndpoint = new ClientEndpoint(monitor,
                new NegotiationController(
                        mockConsumerNegotiationManager(),
                        mock(ContractNegotiationObservable.class),
                        mock(ContractNegotiationStore.class),
                        mockConfig()),
                new PolicyController(
                        monitor,
                        mockCatalogService(),
                        mockTransformer(),
                        mock(Config.class)),
                new DataTransferController(
                        mock(Monitor.class),
                        mockConfig(),
                        mock(WebService.class),
                        mock(PublicApiManagementService.class),
                        mockTransferProcessManager(), ""));
    }

    private Config mockConfig() {
        return ConfigFactory.fromMap(
                Map.of(
                        "edc.dsp.callback.address", "http://localhost:4321/dsp",
                        "web.http.port", "8080",
                        "web.http.path", "/api"));
    }

    private TypeTransformerRegistry mockTransformer() {
        var mockTransformer = mock(TypeTransformerRegistry.class);
        when(mockTransformer.transform(any(), any())).thenReturn(null);
        return mockTransformer;
    }

    private TransferProcessManager mockTransferProcessManager() {
        StatusResult<TransferProcess> mockStatusResult = StatusResult.failure(ResponseStatus.FATAL_ERROR);

        var mockTransferProcessManager = mock(TransferProcessManager.class);
        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
        return mockTransferProcessManager;
    }

    private CatalogService mockCatalogService() throws IOException {
        var catalogService = mock(CatalogService.class);
        var completableFuture = new CompletableFuture<StatusResult<byte[]>>();
        completableFuture.complete(StatusResult.success(new ObjectMapper().writeValueAsBytes(mockCatalog)));

        when(catalogService.requestCatalog(any(), any(), any(), any())).thenReturn(completableFuture);
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
        try (var ignored = clientEndpoint.negotiateContract(
                ContractRequest.Builder.newInstance()
                        .counterPartyAddress(url.toString())
                        .contractOffer(
                                ContractOffer.Builder.newInstance()
                                        .id(UUID.randomUUID().toString())
                                        .policy(mockPolicyDefinition.getPolicy())
                                        .assetId(UUID.randomUUID().toString())
                                        .build())
                        .protocol("dataspace-protocol-http")
                        .build())) {
            fail();
        } catch (EdcException expected) {
            if (!(expected.getCause().getClass().equals(TimeoutException.class) &&
                    expected.getMessage().contains("Agreement"))) {
                fail(); // This must fail because of agreement timeout.
            }
        }
    }

    @Test
    public void negotiateContractAndTransferTest() {
        try (var ignored = clientEndpoint.negotiateContract(url, "test-id", "test-asset-id", null)) {
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
    public void getAcceptedContractOffersTest() {
        assertEquals(Response.Status.OK.getStatusCode(), clientEndpoint.getAcceptedPolicyDefinitions().getStatus());
    }

    @Test
    public void addAcceptedContractOffersTest() {
        var mockPolicyDefinitionsAsList = new ArrayList<PolicyDefinition>();
        mockPolicyDefinitionsAsList.add(mockPolicyDefinition); // ClientEndpoint creates ArrayList
        var offers = new PolicyDefinition[]{mockPolicyDefinition};

        clientEndpoint.addAcceptedPolicyDefinitions(offers);

        assertEquals(mockPolicyDefinitionsAsList, clientEndpoint.getAcceptedPolicyDefinitions().getEntity());
    }

    @Test
    public void updateAcceptedContractOfferTest() {
        var offers = new PolicyDefinition[]{mockPolicyDefinition};

        clientEndpoint.addAcceptedPolicyDefinitions(offers);

        var mockPolicy = Policy.Builder.newInstance().build();
        var mockUpdatedContractOffer = PolicyDefinition.Builder.newInstance()
                .id(mockPolicyDefinition.getId()) // Same id <-> same offer
                .policy(mockPolicy)
                .build();

        var mockContractOfferAsList = new ArrayList<PolicyDefinition>();
        mockContractOfferAsList.add(mockUpdatedContractOffer); // ClientEndpoint creates ArrayList
        clientEndpoint.updateAcceptedPolicyDefinition(mockUpdatedContractOffer);

        assertEquals(mockContractOfferAsList, clientEndpoint.getAcceptedPolicyDefinitions().getEntity());

    }
}
