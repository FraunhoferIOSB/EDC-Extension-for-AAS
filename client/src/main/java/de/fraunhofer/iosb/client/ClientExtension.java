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
package de.fraunhofer.iosb.client;

import static java.lang.String.format;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

import de.fraunhofer.iosb.client.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.client.dataTransfer.DataTransferObservable;
import de.fraunhofer.iosb.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.client.negotiation.Negotiator;
import de.fraunhofer.iosb.client.policy.PolicyService;
import jakarta.ws.rs.core.UriBuilder;

public class ClientExtension implements ServiceExtension {

    @Inject
    private AuthenticationService authenticationService;
    @Inject
    private CatalogService catalogService;
    @Inject
    private TypeTransformerRegistry transformer;
    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private TransferProcessManager transferProcessManager;
    @Inject
    private WebService webService;

    public static final String SETTINGS_PREFIX = "edc.client.";

    private Monitor monitor;

    @Override
    public void initialize(ServiceExtensionContext context) {
        monitor = context.getMonitor();
        var config = context.getConfig();

        var observable = new DataTransferObservable(monitor);
        var authenticationRequestFilter = new CustomAuthenticationRequestFilter(monitor,
                authenticationService);

        var policyService = new PolicyService(monitor, catalogService, transformer, config);

        var negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationObservable,
                contractNegotiationStore, config);

        var uri = createOwnUriFromConfigurationValues(config);
        var transferInitiator = new TransferInitiator(config, authenticationRequestFilter, observable, uri,
                transferProcessManager);

        // TODO split up client Endpoint functionality?
        var endpoint = new ClientEndpoint(monitor, negotiator, policyService, transferInitiator);

        webService.registerResource(endpoint);

    }

    /*
     * TODO Maybe there is another way to retrieve these values?
     */
    private URI createOwnUriFromConfigurationValues(Config config) {
        URL protocolAddress;
        var protocolAddressString = config.getString("edc.dsp.callback.address");

        try {
            protocolAddress = new URL(protocolAddressString);
        } catch (MalformedURLException idsWebhookAddressException) {
            throw new EdcException(
                    format("[Client] Configuration value edc.dsp.callback.address is a malformed URL: %s",
                            protocolAddressString),
                    idsWebhookAddressException);
        }

        int ownPort = Integer.parseInt(config.getString("web.http.port"));
        String ownPath = config.getString("web.http.path");

        var ownUriBuilder = UriBuilder.newInstance()
                .scheme(protocolAddress.getProtocol())
                .host(protocolAddress.getHost())
                .port(ownPort)
                .path(ownPath);

        return ownUriBuilder.build();

    }

}
