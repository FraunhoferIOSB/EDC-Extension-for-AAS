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

import de.fraunhofer.iosb.client.ClientEndpoint;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.transfer.spi.types.TransferRequest;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

import static de.fraunhofer.iosb.client.dataTransfer.DataTransferController.DATA_TRANSFER_API_KEY;
import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.CoreConstants.EDC_NAMESPACE;


/**
 * Initiate transfer requests
 */
class TransferInitiator {

    private final TransferProcessManager transferProcessManager;
    private final Monitor monitor;
    private final URI ownUri;
    private final String connectorId;

    TransferInitiator(Config config, Monitor monitor,
                      TransferProcessManager transferProcessManager, String connectorId) {
        this.monitor = monitor;
        this.ownUri = createOwnUriFromConfigurationValues(config);
        this.transferProcessManager = transferProcessManager;
        this.connectorId = connectorId;
    }

    void initiateTransferProcess(URL providerUrl, String agreementId, String assetId, String apiKey) {
        if (Objects.isNull(ownUri)) {
            monitor.warning(
                    "Cannot transfer to own EDC since own URI could not be built while initializing client extension. Not continuing...");
            return;
        }
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
                .counterPartyAddress(providerUrl.toString())
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .connectorId(this.connectorId)
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
        String protocolAddressString;
        int ownPort;
        String ownPath;
        try {
            protocolAddressString = config.getString("edc.dsp.callback.address");
            ownPort = config.getInteger("web.http.port", -1);
            ownPath = config.getString("web.http.path", null);
        } catch (EdcException noSettingFound) {
            monitor.severe(
                    format("[Client] Could not build own URI, thus cannot transfer data to this EDC. Only data transfers to external endpoints are supported. Exception message: %s",
                            noSettingFound.getMessage()));
            return null;
        }

        // Remove /dsp from URL
        protocolAddressString = protocolAddressString.substring(0, protocolAddressString.length() - "/dsp".length());
        try {
            return UriBuilder
                    .fromUri(protocolAddressString)
                    .port(ownPort)
                    .path(format(
                            "%s/%s/%s",
                            ownPath,
                            ClientEndpoint.AUTOMATED_PATH,
                            DataTransferEndpoint.RECEIVE_DATA_PATH))
                    .build();

        } catch (IllegalArgumentException | UriBuilderException ownUriBuilderException) {
            monitor.severe(
                    format("[Client] Could not build own URI, thus cannot transfer data to this EDC. Only data transfers to external endpoints are supported. Exception message: %s",
                            ownUriBuilderException.getMessage()));
        }
        return null;
    }

}
