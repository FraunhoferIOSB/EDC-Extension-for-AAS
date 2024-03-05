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
package de.fraunhofer.iosb.client.datatransfer;

import de.fraunhofer.iosb.client.authentication.CustomAuthenticationRequestFilter;
import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;

import java.net.URL;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

public class DataTransferController {

    static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";

    private static final int WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT = 10;

    private final Config config;

    private final DataTransferObservable dataTransferObservable;
    private final TransferInitiator transferInitiator;

    private final CustomAuthenticationRequestFilter dataEndpointAuthenticationRequestFilter;

    /**
     * Class constructor
     *
     * @param monitor                Logging.
     * @param config                 Read config value transfer timeout and
     *                               own URI
     * @param webService             Register data transfer endpoint.
     * @param authenticationService  Creating and passing through custom api
     *                               keys for each data transfer.
     * @param transferProcessManager Initiating a transfer process as a
     *                               consumer.
     */
    public DataTransferController(Monitor monitor, Config config, WebService webService,
                                  AuthenticationService authenticationService, TransferProcessManager transferProcessManager) {
        this.config = config;
        this.transferInitiator = new TransferInitiator(config, monitor, transferProcessManager);
        this.dataEndpointAuthenticationRequestFilter = new CustomAuthenticationRequestFilter(monitor,
                authenticationService);

        this.dataTransferObservable = new DataTransferObservable(monitor);
        var dataTransferEndpoint = new DataTransferEndpoint(monitor, dataTransferObservable);
        webService.registerResource(dataTransferEndpoint);
    }

    /**
     * Initiates the transfer process defined by the arguments. The data of the
     * transfer will be sent to {@link DataTransferEndpoint#RECEIVE_DATA_PATH}.
     *
     * @param providerUrl        The provider from whom the data is to be fetched.
     * @param agreementId        Non-null ContractAgreement of the negotiation process.
     * @param assetId            The asset to be fetched.
     * @param dataDestinationUrl HTTPDataAddress the result of the transfer should be
     *                           sent to. (If null, send to extension and print in log)
     * @return A completable future whose result will be the data or an error message.
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
        var waitForTransferTimeout = config.getInteger("getWaitForTransferTimeout",
                WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT);
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var data = dataFuture.get(waitForTransferTimeout, TimeUnit.SECONDS);
            dataTransferObservable.unregister(agreementId);
            return data;
        } catch (TimeoutException transferTimeoutExceededException) {
            dataTransferObservable.unregister(agreementId);
            throw new EdcException(format("Waiting for a transfer failed for agreementId: %s", agreementId),
                    transferTimeoutExceededException);
        }
    }
}
