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
package de.fraunhofer.iosb.app.model.aas;

import de.fraunhofer.iosb.aas.lib.model.impl.Registry;
import de.fraunhofer.iosb.aas.lib.model.impl.Service;
import de.fraunhofer.iosb.app.model.aas.registry.RegistryRepository;
import de.fraunhofer.iosb.app.model.aas.service.ServiceRepository;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AasProviderRepositoryTest {
    private final URL accessUrl = new URL("https://123.45.67.8:1234/api/v4.2");

    AasProviderRepositoryTest() throws MalformedURLException {
    }

    @Test
    void test_create_multipleEqualServices() {
        AasProviderRepository<Service> testSubject = new ServiceRepository();
        assertTrue(testSubject.create(new Service.Builder().withUrl(accessUrl).build()));
        assertFalse(testSubject.create(new Service.Builder().withUrl(accessUrl).build()));
    }

    @Test
    void test_create_multipleEqualRegistries() {
        AasProviderRepository<Registry> testSubject = new RegistryRepository();
        assertTrue(testSubject.create(new Registry(accessUrl)));
        assertFalse(testSubject.create(new Registry(accessUrl)));
    }
}