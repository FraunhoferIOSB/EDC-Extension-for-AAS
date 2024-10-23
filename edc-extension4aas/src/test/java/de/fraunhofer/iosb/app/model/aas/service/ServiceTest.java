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
package de.fraunhofer.iosb.app.model.aas.service;

import de.fraunhofer.iosb.model.aas.service.Service;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ServiceTest {

    @Test
    void testWithReturnsDifferentObjectReference() throws MalformedURLException {
        Service myService = new Service(new URL("http://aas-access-url"));

        var environment = Asset.Builder.newInstance().build();
        Service withedService = myService.with(environment);

        // Compares reference
        assertNotSame(myService, withedService);
        // The "old" service should also not copy the new one's environment
        assertNotEquals(environment, myService.environment());
    }

}