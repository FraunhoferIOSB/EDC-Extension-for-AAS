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

import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.junit.extensions.EmbeddedRuntime;
import org.eclipse.edc.junit.extensions.RuntimePerClassExtension;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;


@ExtendWith(DependencyInjectionExtension.class)
class EdcConnectorClientExtensionTest {

    private static final EmbeddedRuntime RUNTIME = new EmbeddedRuntime("edc-connector-client-initialize-test-runtime",
            "validator:validator-data-address-aas-data",
            "asset-spi",
            "data-plane-aas-spi",
            "http-spi",
            "policy-spi",
            "contract-spi",
            "transform-lib",
            "control-plane-transform",
            "json-ld",
            "runtime-core",
            "connector-core",
            "edc-connector-client")
            .configurationProvider(() -> ConfigFactory.fromMap(Map.of(
                    "edc.controlplane.management.url", "http://localhost:1")));

    @RegisterExtension
    static RuntimePerClassExtension runtime = new RuntimePerClassExtension(RUNTIME);


    @Disabled("loading extensions via EmbeddedRuntime does not work")
    @Test
    void initialize() {
        assert true;
    }
}
