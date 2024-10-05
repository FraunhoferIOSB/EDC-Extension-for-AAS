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

import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.client.authentication.DataTransferEndpointManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.observe.TransferProcessObservable;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.WebService;

import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DataTransferController {

    static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";

    private static final int WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT = 10;

    private final Config config;

    private final DataTransferObservable dataTransferObservable;
    private final TransferInitiator transferInitiator;

    private final DataTransferEndpointManager dataTransferEndpointManager;

    /**
     * Class constructor
     *
     * @param monitor                    Logging.
     * @param config                     Read config value transfer timeout and
     *                                   own URI
     * @param webService                 Register data transfer endpoint.
     * @param publicApiManagementService Creating and passing through custom api
     *                                   keys for each data transfer.
     * @param transferProcessManager     Initiating a transfer process as a
     *                                   consumer.
     */
    public DataTransferController(Monitor monitor, Config config, WebService webService,
                                  PublicApiManagementService publicApiManagementService,
                                  TransferProcessManager transferProcessManager,
                                  TransferProcessObservable transferProcessObservable,
                                  Hostname hostname) {
        this.config = config.getConfig("edc.client");

        transferInitiator = new TransferInitiator(monitor, config, hostname, transferProcessManager);
        dataTransferEndpointManager = new DataTransferEndpointManager(publicApiManagementService);
        dataTransferObservable = new DataTransferObservable(monitor);
        var dataTransferEndpoint = new DataTransferEndpoint(monitor, dataTransferObservable);

        transferProcessObservable.registerListener(dataTransferObservable);
        webService.registerResource(dataTransferEndpoint);
    }

    /**
     * Initiates the transfer process defined by the arguments. The data of the
     * transfer will be sent to {@link DataTransferEndpoint#RECEIVE_DATA_PATH}.
     *
     * @param providerUrl     The provider from whom the data is to be fetched.
     * @param agreementId     Non-null ContractAgreement of the negotiation process.
     * @param dataSinkAddress HTTPDataAddress the result of the transfer should be
     *                        sent to. (If null, send to extension and print in log)
     * @return StatusResult containing error message or data or null on remote destination address
     * @throws InterruptedException If the data transfer was interrupted
     * @throws ExecutionException   If the data transfer process failed
     */
    public StatusResult<String> initiateTransferProcess(URL providerUrl, String agreementId,
                                                        DataAddress dataSinkAddress)
            throws InterruptedException, ExecutionException {
        // Prepare for incoming data
        var dataFuture = dataTransferObservable.register(agreementId);

        if (dataSinkAddress != null) {
            // Send data to custom target url
            this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, dataSinkAddress);
            // Don't have to wait for data
            return StatusResult.success(null);
        }

        var apiKey = UUID.randomUUID().toString();
        dataTransferEndpointManager.addTemporaryEndpoint(agreementId, DATA_TRANSFER_API_KEY, apiKey);

        var initiateResult = this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, apiKey);

        return initiateResult.succeeded() ? waitForData(dataFuture, agreementId) :
                StatusResult.failure(initiateResult.getFailure().status(), initiateResult.getFailureDetail());
    }

    private StatusResult<String> waitForData(CompletableFuture<String> dataFuture, String agreementId)
            throws InterruptedException {
        var waitForTransferTimeout = config.getInteger("waitForTransferTimeout",
                WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT);
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var data = dataFuture.get(waitForTransferTimeout, TimeUnit.SECONDS);
            dataTransferObservable.unregister(agreementId);
            return StatusResult.success(data);
        } catch (TimeoutException | ExecutionException futureException) {
            dataTransferObservable.unregister(agreementId);
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, futureException.getMessage());
        }
    }
}
