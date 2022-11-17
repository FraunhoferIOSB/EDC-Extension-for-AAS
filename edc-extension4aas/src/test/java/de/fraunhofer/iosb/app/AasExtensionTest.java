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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.eclipse.edc.api.auth.spi.AuthenticationService;
import org.eclipse.edc.connector.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.asset.AssetIndex;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.injection.ObjectFactory;
import org.eclipse.edc.web.spi.WebService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import okhttp3.OkHttpClient;

@ExtendWith(DependencyInjectionExtension.class)
public class AasExtensionTest {

    private AasExtension extension;
    private ServiceExtensionContext context;

    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(ContractDefinitionStore.class, mock(ContractDefinitionStore.class));
        context.registerService(AssetIndex.class, mock(AssetIndex.class));
        context.registerService(PolicyDefinitionStore.class, mock(PolicyDefinitionStore.class));
        context.registerService(OkHttpClient.class, mock(OkHttpClient.class));
        context.registerService(WebService.class, mock(WebService.class));
        context.registerService(AuthenticationService.class, mock(AuthenticationService.class));

        this.context = spy(context); // used to inject the config
        when(this.context.getMonitor()).thenReturn(mock(Monitor.class));
        extension = factory.constructInstance(AasExtension.class);
    }

    @Test
    public void initializeTest() {
        // See if initialization works
        extension.initialize(this.context);
    }
}
