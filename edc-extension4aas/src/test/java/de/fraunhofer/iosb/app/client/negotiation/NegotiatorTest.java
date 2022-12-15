package de.fraunhofer.iosb.app.client.negotiation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import org.eclipse.edc.connector.contract.observe.ContractNegotiationObservableImpl;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NegotiatorTest {

    private final ConsumerContractNegotiationManager consumerNegotiationManager = mock(
            ConsumerContractNegotiationManager.class);
    private final ContractNegotiationObservable observable = new ContractNegotiationObservableImpl();
    private Negotiator clientNegotiator;

    @BeforeEach
    void initializeClientNegotiator() throws IOException {
        clientNegotiator = new Negotiator(consumerNegotiationManager, observable);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testNegotiate() throws MalformedURLException, InterruptedException, ExecutionException, URISyntaxException {
        var negotiationStatusResult = mock(StatusResult.class);
        when(negotiationStatusResult.succeeded()).thenReturn(true);
        var contractNegotiation = mock(ContractNegotiation.class);
        when(contractNegotiation.getId()).thenReturn("test-negotiation-id");
        when(negotiationStatusResult.getContent()).thenReturn(contractNegotiation);

        when(consumerNegotiationManager.initiate(any())).thenReturn(negotiationStatusResult);

        var contractOffer = mock(ContractOffer.class);
        when(contractOffer.getProvider()).thenReturn(new URI("test"));

        var agreement = mock(ContractAgreement.class);
        when(contractNegotiation.getContractAgreement()).thenReturn(agreement);
        when(agreement.getId()).thenReturn("agreementId");

        var future = Executors.newSingleThreadExecutor().submit(() -> {
            return clientNegotiator.negotiate(new URL("http://testurl:12345"), contractOffer);
        });
        // Let the negotiator think we need time to process
        // If not, the "confirmed" signal will be sent too soon, and the negotiator will
        // never complete
        Thread.sleep(1000);
        observable.invokeForEach(l -> l.confirmed(contractNegotiation));

        assertEquals(agreement, future.get());
    }
}
