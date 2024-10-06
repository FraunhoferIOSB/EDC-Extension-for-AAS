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
package de.fraunhofer.iosb.registry;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AasServiceRegistryTest {

    final URL testUrl = new URL("http://localhost");

    AasServiceRegistryTest() throws MalformedURLException {
    }

    @Test
    void testCreate() {
        new AasServiceRegistry(null);
        new AasServiceRegistry(new HashSet<>());
    }

    @Test
    void testRegister() {
        var repo = new HashSet<String>();
        var testSubject = new AasServiceRegistry(repo);
        testSubject.register(testUrl);
        assertEquals(1, repo.size());
        assertEquals(testUrl.toString(), repo.iterator().next());
    }

    @Test
    void testUnregister() {
        var repo = new HashSet<String>();
        var testSubject = new AasServiceRegistry(repo);
        testSubject.register(testUrl);
        testSubject.unregister(testUrl);

        assertEquals(0, repo.size());
        assertFalse(repo.iterator().hasNext());
    }

    @Test
    void testUnregisterWrongServiceUrl() throws MalformedURLException {
        var repo = new HashSet<String>();
        var testSubject = new AasServiceRegistry(repo);
        testSubject.register(testUrl);
        // Should not fail
        testSubject.unregister(new URL("http://localhost2"));

        assertEquals(1, repo.size());
        assertEquals(testUrl.toString(), repo.iterator().next());
    }

}