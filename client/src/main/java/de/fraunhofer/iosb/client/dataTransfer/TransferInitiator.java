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
package de.fraunhofer.iosb.client.dataTransfer;

import static de.fraunhofer.iosb.client.dataTransfer.DataTransferController.DATA_TRANSFER_API_KEY;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;

import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.DataAddress;

import de.fraunhofer.iosb.client.ClientEndpoint;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Initiate transfer requests
 */
class TransferInitiator {

    private final TransferProcessManager transferProcessManager;
    private final URI ownUri;

    TransferInitiator(Config config,
            TransferProcessManager transferProcessManager) {

        this.ownUri = createOwnUriFromConfigurationValues(config);
        this.transferProcessManager = transferProcessManager;
    }

    void initiateTransferProcess(URL providerUrl, String agreementId, String assetId, String apiKey) {
        var dataDestination = DataAddress.Builder.newInstance()
                .type("HttpData")
                .property(EDC_NAMESPACE + "baseUrl", ownUri.toString())
                .property(EDC_NAMESPACE + "path", agreementId)
                .property("header:" + DATA_TRANSFER_API_KEY, apiKey) // API key for validation on consumer side
                .build();

        initiateTransferProcess(providerUrl, agreementId, assetId, dataDestination);
    }

    void initiateTransferProcess(URL providerUrl, String agreementId, String assetId, DataAddress dataSinkAddress) {

        var transferRequest = TransferRequest.Builder.newInstance()
                .id(UUID.randomUUID().toString()) // this is not relevant, thus can be random
                .connectorId(providerUrl.toString()) // the address of the provider connector
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .connectorId("consumer")
                .assetId(assetId)
                .dataDestination(dataSinkAddress)
                .contractId(agreementId)
                .build();

        var transferProcessStatus = transferProcessManager.initiateConsumerRequest(transferRequest);
        if (transferProcessStatus.failed()) {
            throw new EdcException(transferProcessStatus.getFailureDetail());
        }
    }

    private URI createOwnUriFromConfigurationValues(Config config) {
        var protocolAddressString = config.getString("edc.dsp.callback.address");
        var ownPort = config.getInteger("web.http.port");
        var ownPath = config.getString("web.http.path");

        return UriBuilder
                .fromUri(protocolAddressString)
                .port(ownPort)
                .path(format(
                        "%s/%s/%s",
                        ownPath,
                        ClientEndpoint.AUTOMATED_PATH,
                        DataTransferEndpoint.RECEIVE_DATA_PATH))
                .build();
    }

}
