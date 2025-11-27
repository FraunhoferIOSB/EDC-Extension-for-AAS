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
package de.fraunhofer.iosb.app;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.connector.controlplane.asset.spi.index.AssetIndex;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.contractdefinition.InMemoryContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.defaults.storage.policydefinition.InMemoryPolicyDefinitionStore;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.query.CriterionOperatorRegistryImpl;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(DependencyInjectionExtension.class)
public class AasExtensionTest {

    private AasExtension extension;


    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(AssetIndex.class, mock(AssetIndex.class));
        context.registerService(ContractDefinitionStore.class, new InMemoryContractDefinitionStore(new CriterionOperatorRegistryImpl()));
        context.registerService(Monitor.class, new ConsoleMonitor());
        context.registerService(PolicyDefinitionStore.class, new InMemoryPolicyDefinitionStore(new CriterionOperatorRegistryImpl()));
        context.registerService(WebService.class, mock(WebService.class));

        when(context.getConfig()).thenReturn(mock(Config.class));

        extension = factory.constructInstance(AasExtension.class);
    }


    @Test
    public void testInitialize(ServiceExtensionContext context) {
        // See if initialization works
        extension.initialize(context);
    }
}
