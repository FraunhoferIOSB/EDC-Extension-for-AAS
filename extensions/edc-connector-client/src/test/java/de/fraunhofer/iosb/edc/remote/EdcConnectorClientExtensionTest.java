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
package de.fraunhofer.iosb.edc.remote;

import de.fraunhofer.iosb.aas.test.defaults.DefaultEdcHttpClient;
import de.fraunhofer.iosb.aas.test.defaults.DefaultVault;
import de.fraunhofer.iosb.codec.Codec;
import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.jsonld.spi.JsonLd;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(DependencyInjectionExtension.class)
class EdcConnectorClientExtensionTest {

    private EdcConnectorClientExtension extension;


    @BeforeEach
    void setup(ServiceExtensionContext context, ObjectFactory factory) {
        context.registerService(Codec.class, new Codec(mock(TypeTransformerRegistry.class), mock(JsonLd.class)));
        context.registerService(EdcHttpClient.class, new DefaultEdcHttpClient());
        context.registerService(Monitor.class, new ConsoleMonitor());
        context.registerService(Vault.class, new DefaultVault());

        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(
                "edc.controlplane.management.url", "http://invalid.local")));

        extension = factory.constructInstance(EdcConnectorClientExtension.class);
    }


    @Test
    public void testInitialize(ServiceExtensionContext context) {
        // See if initialization works
        extension.initialize(context);
    }
}
