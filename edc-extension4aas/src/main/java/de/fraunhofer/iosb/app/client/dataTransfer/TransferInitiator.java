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

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import de.fraunhofer.iosb.app.client.ClientEndpoint;
import jakarta.ws.rs.core.MediaType;

/**
 * Initiate transfer requests
 */
public class TransferInitiator {

    private static final String PROTOCOL_IDS_MULTIPART = "ids-multipart";

    private final TransferProcessManager transferProcessManager;
    private final URI ownUri;

    /**
     * Class constructor
     * 
     * @param ownUri                 URL of this EDC.
     * @param transferProcessManager Initiating a transfer process as a
     *                               consumer.
     */
    public TransferInitiator(URI ownUri,
            TransferProcessManager transferProcessManager) {
        this.ownUri = ownUri
                .resolve(format("./%s/%s/%s", ownUri.getPath(), ClientEndpoint.AUTOMATED_PATH,
                        ClientEndpoint.RECEIVE_DATA_PATH));
        this.transferProcessManager = transferProcessManager;
    }

    /**
     * Initiates the transfer process defined by the arguments. The data of the
     * transfer will be sent to {@link ClientEndpoint#RECEIVE_DATA_PATH}.
     * 
     * @param providerUrl Non-null URL of provider service that should send the data.
     * @param agreementId Non-null agreementId of the negotiation process.
     * @param assetId     Non-null asset whose data is to be transferred.
     */
    public void initiateTransferProcess(URL providerUrl, String agreementId, String assetId) {

        var dataRequest = DataRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString()) // this is not relevant, thus can be random
                .connectorAddress(providerUrl.toString()) // the address of the provider connector
                .protocol(PROTOCOL_IDS_MULTIPART)
                .connectorId("consumer")
                .assetId(assetId)
                .dataDestination(HttpDataAddress.Builder.newInstance()
                        .baseUrl(new URIBuilder(ownUri).addParameter("agreementId", agreementId).toString())
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
    }

}
