package de.fraunhofer.iosb.app.client;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

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

public class ClientEndpointTest {

    private static int port;
    private static URL url;
    private static ClientAndServer mockServer;

    private ClientEndpoint clientEndpoint;

    @BeforeAll
    public static void initialize() throws IOException {
        Logger.getInstance().setMonitor(mock(Monitor.class));
        port = 8080;
        url = new URL(format("http://localhost:%s", port));
        mockServer = startClientAndServer(port);
    }

    @BeforeEach
    public void setupSynchronizer() {
        clientEndpoint = new ClientEndpoint(URI.create("http://localhost:8181/api"),
                mock(CatalogService.class), mockConsumerNegotiationManager(),
                mock(ContractNegotiationStore.class),
                mock(ContractNegotiationObservable.class), mock(TransferProcessManager.class),
                mock(DataTransferObservable.class), mock(CustomAuthenticationRequestFilter.class));
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
        var mockAsset = Asset.Builder.newInstance().id("test-asset").build();
        var mockPolicy = Policy.Builder.newInstance().build();
        var mockedContractStart = ZonedDateTime.now();
        var mockedContractEnd = ZonedDateTime.of(mockedContractStart.toLocalDate(),
                mockedContractStart.toLocalTime().plusHours(1), mockedContractStart.getZone());
        try {
            clientEndpoint.negotiateContract(url,
                    ContractOffer.Builder.newInstance()
                            .id("test-contract-offer")
                            .asset(mockAsset)
                            .policy(mockPolicy)
                            .contractStart(mockedContractStart)
                            .contractEnd(mockedContractEnd)
                            .provider(URI.create(url.toString()))
                            .build());
            fail();
        } catch (EdcException expected) {
            if (!(expected.getCause().getClass().equals(TimeoutException.class)
                    && expected.getMessage().contains("agreement"))) {
                fail(); // This must fail because of agreement timeout.
            }
        }
    }
}
