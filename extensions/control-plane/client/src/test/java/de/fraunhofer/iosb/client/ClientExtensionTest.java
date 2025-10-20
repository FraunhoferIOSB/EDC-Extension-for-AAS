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

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.connector.controlplane.transfer.spi.TransferProcessManager;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;

@ExtendWith(DependencyInjectionExtension.class)
public class ClientExtensionTest {

    private ClientExtension clientExtension;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));
        context.registerService(CatalogService.class, mock(CatalogService.class));
        context.registerService(ConsumerContractNegotiationManager.class,
                mock(ConsumerContractNegotiationManager.class));
        context.registerService(ContractNegotiationObservable.class, mock(ContractNegotiationObservable.class));
        context.registerService(ContractNegotiationStore.class, mock(ContractNegotiationStore.class));
        context.registerService(TransferProcessManager.class, mock(TransferProcessManager.class));
        context.registerService(WebService.class, mock(WebService.class));
        context.registerService(Monitor.class, new ConsoleMonitor());

        clientExtension = factory.constructInstance(ClientExtension.class);
    }

    @Test
    public void initializeTest(ServiceExtensionContext context) {
        // See if initializing the extension works
        clientExtension.initialize(context);
    }

}
