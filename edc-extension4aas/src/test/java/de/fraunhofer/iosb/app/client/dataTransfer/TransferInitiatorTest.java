package de.fraunhofer.iosb.app.client.dataTransfer;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.StatusResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransferInitiatorTest {

    private URI ownUri;
    private TransferProcessManager mockTransferProcessManager = mock(TransferProcessManager.class);
    private TransferInitiator transferInitiator;
    private StatusResult<String> mockStatusResult;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void initializeContractOfferService() throws IOException, URISyntaxException {
        ownUri = new URI("http://localhost:4321/api/ids");
        transferInitiator = new TransferInitiator(ownUri, mockTransferProcessManager,
                mock(DataTransferObservable.class));
        mockStatusResult = mock(StatusResult.class);
        when(mockTransferProcessManager.initiateConsumerRequest(any())).thenReturn(mockStatusResult);
    }

    @Test
    void testInitiateTransferProcess() throws MalformedURLException {
        when(mockStatusResult.failed()).thenReturn(false);
        transferInitiator.initiateTransferProcess(new URL("http://provider-url:1234"), "test-agreement-id",
        "test-asset");
        verify(mockTransferProcessManager, times(1)).initiateConsumerRequest(any());
    }

    @Test
    void testInitiateTransferProcessThrowsEdcExceptionOnFailedTransferInitiation() throws MalformedURLException {
        when(mockStatusResult.failed()).thenReturn(true);
        try {
        transferInitiator.initiateTransferProcess(new URL("http://provider-url:1234"), "test-agreement-id",
        "test-asset");
        fail();
        } catch(EdcException expected){
        }
    }
}
