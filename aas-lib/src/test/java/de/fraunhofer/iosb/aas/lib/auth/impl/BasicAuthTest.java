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
package de.fraunhofer.iosb.aas.lib.auth.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BasicAuthTest {

    private final String username = "my-username-x7h9e5s9h";
    private final String password = "my-password-34kj67j4h2g6";
    private BasicAuth testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new BasicAuth(username, password);
    }

    @AfterEach
    void tearDown() {
        testSubject = null;
    }

    @Test
    void getValue() {
        String unencodedAuthString = "Basic: %s:%s".formatted(username, password);
        String encodedAuthString = Base64.getEncoder().encodeToString(unencodedAuthString.getBytes(StandardCharsets.UTF_8));
        assertEquals(encodedAuthString, testSubject.getValue());
    }

    @Test
    void getHeader() {
        String unencodedAuthString = "Basic: %s:%s".formatted(username, password);
        String encodedAuthString = Base64.getEncoder().encodeToString(unencodedAuthString.getBytes(StandardCharsets.UTF_8));
        assertEquals(encodedAuthString, testSubject.getHeader().getValue());
        assertEquals("Authorization", testSubject.getHeader().getKey());

    }
}