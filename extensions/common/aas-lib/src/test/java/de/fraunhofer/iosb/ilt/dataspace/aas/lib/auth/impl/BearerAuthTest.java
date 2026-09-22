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

import de.fraunhofer.iosb.ilt.dataspace.aas.test.defaults.DefaultVault;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


class BearerAuthTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";
    private static final URI IDENTITY_PROVIDER = URI.create("http://idp.example.local/token");
    private static final String TOKEN = "my-bearer-token-1234";

    private final Vault vault = new DefaultVault();
    private BearerAuth testSubject;


    @BeforeEach
    void setUp() {
        Oauth2Client oauth2Client = mock(Oauth2Client.class);
        when(oauth2Client.requestToken(any()))
                .thenReturn(Result.success(TokenRepresentation.Builder.newInstance().token(TOKEN).build()));
        testSubject = new BearerAuth(CLIENT_ID, CLIENT_SECRET, IDENTITY_PROVIDER, oauth2Client, vault);
    }


    @Test
    void getKey_returnsAuthorization() {
        assertEquals("Authorization", testSubject.getKey());
    }


    @Test
    void getValue_returnsBearerToken() {
        assertEquals("Bearer " + TOKEN, testSubject.getValue(vault));
    }


    @Test
    void decorate_setsOauth2Properties() {
        var builder = HttpDataAddress.Builder.newInstance()
                .baseUrl("http://test.local");
        testSubject.decorate(builder);
        var address = builder.build();

        assertEquals(IDENTITY_PROVIDER, address.getProperty("oauth2:tokenUrl"));
        assertEquals(CLIENT_ID, address.getStringProperty("oauth2:clientId"));
        assertEquals(CLIENT_SECRET, vault.resolveSecret(address.getStringProperty("oauth2:clientSecretKey")));
    }
}
