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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static de.fraunhofer.iosb.client.ClientEndpoint.MISSING_QUERY_PARAMETER_MESSAGE;


@Consumes({ MediaType.APPLICATION_JSON })
@Path(ClientEndpoint.AUTOMATED_PATH)
public class DataTransferController {

    public static final String OPERATION_FIELD = "operation";
    static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";
    static final String TRANSFER_PATH = "transfer";
    private static final int WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT = 20;
    private final Config config;

    private final DataTransferObservable<String> dataTransferObservable;
    private final TransferInitiator transferInitiator;
    private final Monitor monitor;

    private final DataTransferEndpointManager dataTransferEndpointManager;

    private final ObjectMapper nonNullNonEmptyObjectMapper;


    /**
     * Class constructor
     *
     * @param monitor Logging.
     * @param config Read config value transfer timeout and own URI
     * @param webService Register data transfer endpoint.
     * @param publicApiManagementService Creating and passing through custom api keys for each data transfer.
     * @param transferProcessManager Initiating a transfer process as a consumer.
     */
    public DataTransferController(Monitor monitor, Config config, WebService webService,
                                  PublicApiManagementService publicApiManagementService,
                                  TransferProcessManager transferProcessManager,
                                  TransferProcessObservable transferProcessObservable,
                                  Hostname hostname) {
        this.config = config.getConfig("edc.client");

        this.monitor = monitor.withPrefix("DataTransferController");

        transferInitiator = new TransferInitiator(monitor, config, hostname, transferProcessManager);
        dataTransferEndpointManager = new DataTransferEndpointManager(publicApiManagementService);
        dataTransferObservable = new DataTransferObservable<>(monitor);
        var dataTransferEndpoint = new DataTransferEndpoint(monitor, dataTransferObservable);
        nonNullNonEmptyObjectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        transferProcessObservable.registerListener(dataTransferObservable);
        webService.registerResource(dataTransferEndpoint);
    }


    /**
     * Submits a data transfer request to the providerUrl. In the future this could be replaced with the
     * <a href="https://www.ietf.org/archive/id/draft-ietf-httpbis-safe-method-w-body-02.html">HTTP QUERY method</a>
     *
     * @param providerUrl The data provider's url
     * @param agreementId The basis of the data transfer.
     * @param dataAddress URL of destination data sink.
     * @return On success, the data of the desired asset. Else, returns an error message.
     */
    @POST
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URI providerUrl,
                            @QueryParam("agreementId") String agreementId,
                            DataAddress dataAddress) {
        monitor.info("POST /%s".formatted(TRANSFER_PATH));
        if (providerUrl == null || agreementId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("providerUrl, agreementId"))
                    .build();
        }
        monitor.debug("providerUrl: %s".formatted(providerUrl.toString()));
        monitor.debug("agreementId: %s".formatted(agreementId));

        try {
            if (dataAddress == null) {
                var tpResult = initiateTransferProcess(providerUrl, agreementId);
                return tpResult.succeeded() ? Response.ok(tpResult.getContent()).build() :
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity(tpResult.getFailureDetail())
                                .build();
            }

            var op = dataAddress.getProperties().get("operation");
            if (op != null) {
                try {
                    dataAddress = serializeOperation(dataAddress);
                }
                catch (JsonProcessingException e) {
                    // Operation invocation is required by client -> return
                    return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
                }
            }

            var dataResult = initiateTransferProcess(providerUrl, agreementId, dataAddress);

            if (dataResult.succeeded()) {
                return Response.ok(dataResult.getContent()).build();
            }
            return Response.status(Response.Status.EXPECTATION_FAILED).entity(dataResult.getFailureDetail()).build();

        }
        catch (InterruptedException | ExecutionException futureException) {
            monitor.severe("Data transfer failed for provider %s and agreementId %s".formatted(providerUrl,
                    agreementId), futureException);
            return Response.serverError()
                    .entity(futureException.getMessage())
                    .build();
        }
    }


    private DataAddress serializeOperation(DataAddress dataAddress) throws JsonProcessingException {
        String operation = nonNullNonEmptyObjectMapper
                .writeValueAsString(dataAddress.getProperties().get(OPERATION_FIELD));

        // Rebuild DA with serialized operation
        dataAddress = DataAddress.Builder.newInstance()
                .type(dataAddress.getType())
                .properties(dataAddress.getProperties())
                .property(OPERATION_FIELD, operation) // This overwrites Operation object with string
                .build();

        return dataAddress;
    }


    /**
     * Initiates the transfer process defined by the arguments. The data of the transfer will be sent to {@link DataTransferEndpoint#RECEIVE_DATA_PATH}.
     *
     * @param providerUri The provider from whom the data is to be fetched.
     * @param agreementId Non-null ContractAgreement of the negotiation process.
     * @param dataSinkAddress DataAddress the result of the transfer should be sent to. (If null, send to extension and print in log)
     * @return StatusResult containing error message or data or null on remote destination address
     * @throws InterruptedException If the data transfer was interrupted
     * @throws ExecutionException If the data transfer process failed
     */
    private StatusResult<String> initiateTransferProcess(URI providerUri, String agreementId,
                                                         DataAddress dataSinkAddress)
            throws InterruptedException, ExecutionException {
        if (dataSinkAddress == null) {
            return initiateTransferProcess(providerUri, agreementId);
        }

        transferInitiator.initiateTransferProcess(providerUri, agreementId, dataSinkAddress);
        // Don't have to wait for data
        return StatusResult.success(null);
    }


    /* Send result of transferProcess to extension endpoint */
    private StatusResult<String> initiateTransferProcess(URI providerUri, String agreementId)
            throws ExecutionException, InterruptedException {
        // Prepare for incoming data
        var providerDataFuture = dataTransferObservable.register(agreementId);

        var apiKey = UUID.randomUUID().toString();
        dataTransferEndpointManager.addTemporaryEndpoint(agreementId, DATA_TRANSFER_API_KEY, apiKey);

        var initiateResult = transferInitiator.initiateTransferProcess(providerUri, agreementId, apiKey);

        return initiateResult.succeeded() ? waitForProviderData(providerDataFuture, agreementId) :
                StatusResult.failure(initiateResult.getFailure().status(), initiateResult.getFailureDetail());
    }


    private StatusResult<String> waitForProviderData(CompletableFuture<String> dataFuture, String agreementId)
            throws InterruptedException, ExecutionException {
        var waitForTransferTimeout = config.getInteger("waitForTransferTimeout",
                WAIT_FOR_TRANSFER_TIMEOUT_DEFAULT);
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var providerData = dataFuture.get(waitForTransferTimeout, TimeUnit.SECONDS);
            dataTransferObservable.unregister(agreementId);
            return StatusResult.success(providerData);
        }
        catch (TimeoutException futureException) {
            dataTransferObservable.unregister(agreementId);

            var errorMessage = Objects.requireNonNullElse(futureException.getMessage(), "No error message");
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, errorMessage);
        }
    }
}
