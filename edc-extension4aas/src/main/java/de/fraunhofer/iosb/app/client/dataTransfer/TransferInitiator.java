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
package de.fraunhofer.iosb.app.client.dataTransfer;

import static java.lang.String.format;

import java.net.URI;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.client.ClientEndpoint;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.core.MediaType;

/**
 * Initiate transfer requests
 */
public class TransferInitiator {

    private static final String DATA_TRANSFER_API_KEY = "data-transfer-api-key";
    private static final String PROTOCOL_IDS_MULTIPART = "ids-multipart";

    private final DataTransferObservable observable;
    private final TransferProcessManager transferProcessManager;
    private final URI ownUri;
    private final CustomAuthenticationRequestFilter dataEndpointAuthenticationRequestFilter;

    /**
     * Class constructor
     * 
     * @param ownUri                                  URL of this EDC.
     * @param transferProcessManager                  Initiating a transfer process
     *                                                as a
     *                                                consumer.
     * @param dataEndpointAuthenticationRequestFilter
     */
    public TransferInitiator(URI ownUri,
            TransferProcessManager transferProcessManager, DataTransferObservable observable,
            CustomAuthenticationRequestFilter dataEndpointAuthenticationRequestFilter) {
        this.ownUri = ownUri
                .resolve(format("./%s/%s/%s", ownUri.getPath(), ClientEndpoint.AUTOMATED_PATH,
                        DataTransferEndpoint.RECEIVE_DATA_PATH));
        this.transferProcessManager = transferProcessManager;
        this.observable = observable;
        this.dataEndpointAuthenticationRequestFilter = dataEndpointAuthenticationRequestFilter;
    }

    /**
     * Initiates the transfer process defined by the arguments. The data of the
     * transfer will be sent to {@link ClientEndpoint#RECEIVE_DATA_PATH}.
     * 
     * @param agreement Non-null ContractAgreement of the negotiation process.
     */
    public CompletableFuture<String> initiateTransferProcess(URL providerUrl, String agreementId, String assetId) {
        // Prepare for incoming data
        var dataFuture = new CompletableFuture<String>();
        observable.register(dataFuture, agreementId);
        var apiKey = String.valueOf(agreementId.hashCode());
        dataEndpointAuthenticationRequestFilter.addTemporaryApiKey(DATA_TRANSFER_API_KEY, apiKey);
        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString()) // this is not relevant, thus can be random
                .connectorAddress(providerUrl.toString()) // the address of the provider connector
                .protocol(PROTOCOL_IDS_MULTIPART)
                .connectorId("consumer")
                .assetId(assetId)
                .dataDestination(HttpDataAddress.Builder.newInstance()
                        .baseUrl(new URIBuilder(ownUri).addParameter("agreementId", agreementId).toString())
                        .addAdditionalHeader(DATA_TRANSFER_API_KEY, apiKey) // API key for validation on consumer side
                        .build())
                .transferType(
                        TransferType.Builder.transferType().contentType(MediaType.APPLICATION_JSON)
                                .isFinite(true).build())
                .managedResources(false) // we do not need any provisioning
                .contractId(agreementId)
                .build();

        var transferProcessStatus = transferProcessManager.initiateConsumerRequest(dataRequest);
        if (transferProcessStatus.failed()) {
            throw new EdcException(transferProcessStatus.getFailureDetail());
        }
        return dataFuture;
    }

    /**
     * Call this with a future received by initiateTransferProcess()
     * 
     * @param dataFuture  Data future created by initiateTransferProcess method
     * @param agreementId AgreementId corresponding to this transfer
     * @return The data
     * @throws InterruptedException If the future was interrupted
     * @throws ExecutionException   If the data transfer process failed
     */
    public String waitForData(CompletableFuture<String> dataFuture, String agreementId)
            throws InterruptedException, ExecutionException {
        try {
            // Fetch TransferTimeout everytime to adapt to runtime config changes
            var data = dataFuture.get(Configuration.getInstance().getWaitForTransferTimeout(), TimeUnit.SECONDS);
            observable.unregister(agreementId);
            return data;
        } catch (TimeoutException transferTimeoutExceededException) {
            observable.unregister(agreementId);
            throw new EdcException(format("Waiting for an transfer failed for agreementId: %s", agreementId),
                    transferTimeoutExceededException);
        }
    }
}
