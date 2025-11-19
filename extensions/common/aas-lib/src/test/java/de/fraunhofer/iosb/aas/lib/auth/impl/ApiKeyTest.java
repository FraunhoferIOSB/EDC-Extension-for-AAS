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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ApiKeyTest {

    private final String keyName = "my-api-key-name";
    private final String keyValue = "my-api-key-value";
    private AuthenticationMethod testSubject;


    @BeforeEach
    void setUp() {
        testSubject = new ApiKey(keyName, keyValue);
    }


    @AfterEach
    void tearDown() {
        testSubject = null;
    }


    @Test
    void getHeader() {
        assertEquals(keyName, Objects.requireNonNull(testSubject.getHeader()).getKey());
        assertEquals(keyValue, testSubject.getHeader().getValue());
    }
}
