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

import de.fraunhofer.iosb.client.ClientEndpoint;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferRequest;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.system.Hostname;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

import static de.fraunhofer.iosb.client.datatransfer.DataTransferController.DATA_TRANSFER_API_KEY;
import static java.lang.String.join;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;
import static org.eclipse.edc.spi.types.domain.transfer.FlowType.PUSH;

/**
 * Initiate transfer requests
 */
class TransferInitiator {

    public static final String COULD_NOT_BUILD_URI_MESSAGE = "[Client] Could not build own URI, cannot transfer data " +
            "to this EDC directly. Only data transfers to external endpoints are supported.";
    public static final String HTTPS_KEYSTORE_PATH = "edc.web.https.keystore.path";
    public static final String HTTP_PORT = "web.http.port";
    public static final String HTTP_PATH = "web.http.path";

    private final Monitor monitor;
    private final TransferProcessManager transferProcessManager;
    private final URI ownUri;

    TransferInitiator(Monitor monitor, Config config, Hostname hostname, TransferProcessManager transferProcessManager) {
        this.monitor = monitor;
        this.transferProcessManager = transferProcessManager;
        this.ownUri = createOwnUriFromConfigurationValues(config, hostname);
    }

    StatusResult<TransferProcess> initiateTransferProcess(URL providerUrl, String agreementId, String apiKey) {
        if (Objects.isNull(ownUri)) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, COULD_NOT_BUILD_URI_MESSAGE);
        }
        monitor.debug("Starting transfer process for provider: " + providerUrl.toString());
        monitor.debug("agreementId: " + agreementId);
        var dataDestination = HttpDataAddress.Builder.newInstance()
                .baseUrl(ownUri.toString())
                .path(agreementId)
                .addAdditionalHeader(DATA_TRANSFER_API_KEY, apiKey) // API key for validation on consumer side
                .build();

        return initiateTransferProcess(providerUrl, agreementId, dataDestination);
    }

    StatusResult<TransferProcess> initiateTransferProcess(URL providerUrl, String agreementId,
                                                          DataAddress dataSinkAddress) {
        var transferRequest = TransferRequest.Builder.newInstance()
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .counterPartyAddress(providerUrl.toString())
                .contractId(agreementId)
                .transferType(join("-", dataSinkAddress.getType(), PUSH.name()))
                .dataDestination(dataSinkAddress)
                .build();

        return transferProcessManager.initiateConsumerRequest(transferRequest);
    }

    private URI createOwnUriFromConfigurationValues(Config config, Hostname hostname) {
        try {
            // HTTPS requires this value. With this configuration variable set, the connector will run with HTTPS
            // enabled
            var uriString = "%s://%s:%s%s/%s/%s".formatted(
                    config.getString(HTTPS_KEYSTORE_PATH, null) == null ? Protocol.HTTP.name() : Protocol.HTTPS.name(),
                    hostname.get(),
                    config.getInteger(HTTP_PORT),
                    config.getString(HTTP_PATH),
                    ClientEndpoint.AUTOMATED_PATH,
                    DataTransferEndpoint.RECEIVE_DATA_PATH);

            return new URI(uriString);
        } catch (URISyntaxException | EdcException couldNotBuildException) {
            monitor.warning(COULD_NOT_BUILD_URI_MESSAGE, couldNotBuildException);
            return null;
        }
    }

    private enum Protocol { HTTP, HTTPS }
}
