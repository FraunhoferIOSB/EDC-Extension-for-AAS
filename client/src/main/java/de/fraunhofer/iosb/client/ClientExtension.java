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

import de.fraunhofer.iosb.api.PublicApiManagementService;
import de.fraunhofer.iosb.client.dataTransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.web.spi.WebService;

public class ClientExtension implements ServiceExtension {

    // Non-public unified authentication request filter management service
    @Inject
    private PublicApiManagementService publicApiManagementService;

    @Inject
    private CatalogService catalogService;
    @Inject
    private ConsumerContractNegotiationManager consumerNegotiationManager;
    @Inject
    private ContractNegotiationObservable contractNegotiationObservable;
    @Inject
    private ContractNegotiationStore contractNegotiationStore;
    @Inject
    private TransferProcessManager transferProcessManager;
    @Inject
    private TypeTransformerRegistry transformer;
    @Inject
    private WebService webService;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        var config = context.getConfig("edc.client");

        var policyController = new PolicyController(monitor, catalogService, transformer, config);

        var negotiationController = new NegotiationController(consumerNegotiationManager,
                contractNegotiationObservable, contractNegotiationStore, config);

        var dataTransferController = new DataTransferController(monitor, context.getConfig(), webService,
                publicApiManagementService, transferProcessManager, context.getConnectorId());

        webService.registerResource(new ClientEndpoint(monitor, negotiationController, policyController,
                dataTransferController));
    }

}
