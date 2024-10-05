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
import de.fraunhofer.iosb.client.ClientEndpoint;
import de.fraunhofer.iosb.client.authentication.DataTransferEndpointManager;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.fraunhofer.iosb.client.ClientEndpoint.MISSING_QUERY_PARAMETER_MESSAGE;

@Consumes({MediaType.APPLICATION_JSON})
@Path(ClientEndpoint.AUTOMATED_PATH)
public class DataTransferController {

    static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";
    static final String TRANSFER_PATH = "transfer";

    private static final int WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT = 10;

    private final Config config;

    private final DataTransferObservable dataTransferObservable;
    private final TransferInitiator transferInitiator;
    private final Monitor monitor;

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

        this.monitor = monitor.withPrefix("Client PolicyController");

        transferInitiator = new TransferInitiator(monitor, config, hostname, transferProcessManager);
        dataTransferEndpointManager = new DataTransferEndpointManager(publicApiManagementService);
        dataTransferObservable = new DataTransferObservable(monitor);
        var dataTransferEndpoint = new DataTransferEndpoint(monitor, dataTransferObservable);

        transferProcessObservable.registerListener(dataTransferObservable);
        webService.registerResource(dataTransferEndpoint);
    }


    /**
     * Submits a data transfer request to the providerUrl.
     * In the future this could be replaced with the
     * <a href="https://www.ietf.org/archive/id/draft-ietf-httpbis-safe-method-w-body-02.html">HTTP QUERY method</a>
     *
     * @param providerUrl The data provider's url
     * @param agreementId The basis of the data transfer.
     * @param dataAddress URL of destination data sink.
     * @return On success, the data of the desired asset. Else, returns an error message.
     */
    @POST
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
                            @QueryParam("agreementId") String agreementId,
                            DataAddress dataAddress) {
        monitor.info("GET /%s".formatted(TRANSFER_PATH));
        if (providerUrl == null || agreementId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("providerUrl, agreementId")).build();
        }

        try {
            var data = initiateTransferProcess(providerUrl, agreementId, dataAddress);
            if (dataAddress == null) {
                return Response.ok(data).build();
            } else {
                return Response.ok("Data transfer request sent.").build();
            }
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe("Data transfer failed for provider %s and agreementId %s".formatted(providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(negotiationException.getMessage())
                    .build();
        }
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
        var providerDataFuture = dataTransferObservable.register(agreementId);

        if (dataSinkAddress != null) {
            // Send data to custom target url
            this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, dataSinkAddress);
            // Don't have to wait for data
            return StatusResult.success(null);
        }

        var apiKey = UUID.randomUUID().toString();
        dataTransferEndpointManager.addTemporaryEndpoint(agreementId, DATA_TRANSFER_API_KEY, apiKey);

        var initiateResult = this.transferInitiator.initiateTransferProcess(providerUrl, agreementId, apiKey);

        return initiateResult.succeeded() ? waitForProviderData(providerDataFuture, agreementId) :
                StatusResult.failure(initiateResult.getFailure().status(), initiateResult.getFailureDetail());
    }

    private StatusResult<String> waitForProviderData(CompletableFuture<String> dataFuture, String agreementId)
            throws InterruptedException {
        var waitForTransferTimeout = config.getInteger("waitForTransferTimeout",
                WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT);
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var providerData = dataFuture.get(waitForTransferTimeout, TimeUnit.SECONDS);
            dataTransferObservable.unregister(agreementId);
            return StatusResult.success(providerData);
        } catch (TimeoutException | ExecutionException futureException) {
            dataTransferObservable.unregister(agreementId);

            var errorMessage = Objects.requireNonNullElse(futureException.getMessage(), "No error message");
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMessage);
        }
    }
}
