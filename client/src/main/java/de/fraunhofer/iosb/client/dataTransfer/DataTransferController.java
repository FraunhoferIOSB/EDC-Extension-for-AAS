package de.fraunhofer.iosb.client.dataTransfer;

import static de.fraunhofer.iosb.client.ClientExtension.SETTINGS_PREFIX;
import static java.lang.String.format;

import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;

import de.fraunhofer.iosb.client.authentication.CustomAuthenticationRequestFilter;

public class DataTransferController {

    static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";

    private static final int WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT = 10;

    private final Config config;

    private final DataTransferEndpoint dataTransferEndpoint;
    private final DataTransferObservable dataTransferObservable;
    private final TransferInitiator transferInitiator;

    private final CustomAuthenticationRequestFilter dataEndpointAuthenticationRequestFilter;

    /**
     * Class constructor
     *
     * @param monitor                       Logging.
     * @param config                        Read config value transfer timeout and own URI
     * @param webService                    Register data transfer endpoint.
     * @param dataEndpointAuthRequestFilter Creating and passing through custom api
     *                                      keys for each data transfer.
     * @param transferProcessManager        Initiating a transfer process as a
     *                                      consumer.
     */
    public DataTransferController(Monitor monitor, Config config, WebService webService,
            AuthenticationService authenticationService, TransferProcessManager transferProcessManager) {
        this.config = config;
        this.transferInitiator = new TransferInitiator(config, transferProcessManager);
        this.dataEndpointAuthenticationRequestFilter = new CustomAuthenticationRequestFilter(monitor,
                authenticationService);

        this.dataTransferObservable = new DataTransferObservable(monitor);
        this.dataTransferEndpoint = new DataTransferEndpoint(monitor, dataTransferObservable);
        webService.registerResource(dataTransferEndpoint);
    }

    /**
     * Initiates the transfer process defined by the arguments. The data of the
     * transfer will be sent to {@link DataTransferEndpoint#RECEIVE_DATA_PATH}.
     *
     * @param providerUrl     The provider from whom the data is to be fetched.
     * @param agreementId     Non-null ContractAgreement of the negotiation process.
     * @param assetId         The asset to be fetched.
     * @param dataSinkAddress HTTPDataAddress the result of the transfer should be
     *                        sent to. (If null, send to extension and print in log)
     * 
     * @return A completable future whose result will be the data or an error
     *         message.
     * @throws InterruptedException If the data transfer was interrupted
     * @throws ExecutionException   If the data transfer process failed
     */
    public String initiateTransferProcess(URL providerUrl, String agreementId, String assetId,
            URL dataDestinationUrl) throws InterruptedException, ExecutionException {
        // Prepare for incoming data
        var dataFuture = new CompletableFuture<String>();
        dataTransferObservable.register(dataFuture, agreementId);

        if (Objects.isNull(dataDestinationUrl)) {
            var apiKey = UUID.randomUUID().toString();
            dataEndpointAuthenticationRequestFilter.addTemporaryApiKey(DATA_TRANSFER_API_KEY, apiKey);

            this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId, apiKey);
            return waitForData(dataFuture, agreementId);
        } else {
            var dataSinkAddress = HttpDataAddress.Builder.newInstance()
                    .baseUrl(dataDestinationUrl.toString())
                    .build();

            this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId, dataSinkAddress);
            return null;
        }

    }

    private String waitForData(CompletableFuture<String> dataFuture, String agreementId)
            throws InterruptedException, ExecutionException {
        var waitForTransferTimeout = config.getInteger(SETTINGS_PREFIX + "getWaitForTransferTimeout",
                WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT);
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var data = dataFuture.get(waitForTransferTimeout, TimeUnit.SECONDS);
            dataTransferObservable.unregister(agreementId);
            return data;
        } catch (TimeoutException transferTimeoutExceededException) {
            dataTransferObservable.unregister(agreementId);
            throw new EdcException(format("Waiting for an transfer failed for agreementId: %s", agreementId),
                    transferTimeoutExceededException);
        }
    }
}
