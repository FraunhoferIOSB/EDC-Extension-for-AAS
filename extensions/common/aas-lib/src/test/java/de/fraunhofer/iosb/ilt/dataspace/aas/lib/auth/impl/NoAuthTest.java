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
package de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.ilt.dataspace.aas.test.defaults.DefaultVault;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class NoAuthTest {

    private AuthenticationMethod testSubject;
    private final Vault vault = new DefaultVault();


    @BeforeEach
    void setUp() {
        testSubject = new NoAuth();
    }


    @Test
    void getKey_returnsNull() {
        assertNull(testSubject.getKey());
    }


    @Test
    void getValue_returnsNull() {
        assertNull(testSubject.getValue(vault));
    }


    @Test
    void decorate_doesNotAddAnyProperties() {
        var builder = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://test.local");
        testSubject.decorate(builder);
        var address = builder.build();

        assertEquals("HttpData", address.getType());
        assertNull(address.getAuthKey());
        assertNull(address.getSecretName());
    }
}
