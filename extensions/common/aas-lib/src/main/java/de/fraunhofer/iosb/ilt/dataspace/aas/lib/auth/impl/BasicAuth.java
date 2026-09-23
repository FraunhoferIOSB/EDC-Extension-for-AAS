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
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.security.Vault;

import java.net.http.HttpClient;
import java.util.Base64;


/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc7617#section-2">rfc7617</a>
 * -> Basic b64(user+":"+password)
 */
public class BasicAuth extends AuthenticationMethod {

    private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();

    private final String passwordAlias;


    /**
     * Creates a new BasicAuth authentication.
     *
     * @param username the username.
     * @param password the password.
     * @param vault the vault to store the password in.
     */
    public BasicAuth(String username, String password, Vault vault) {
        this.passwordAlias = store(vault, String.format("Basic %s", BASE64_ENCODER.encodeToString(String.format("%s:%s", username, password).getBytes())));
    }


    @Override
    public void decorate(HttpDataAddress.Builder addressBuilder) {
        addressBuilder.authKey("Authorization");
        addressBuilder.secretName(passwordAlias);
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor(Vault vault) {
        throw new RuntimeException("Use auth header instead");
    }


    @Override
    public String getKey() {
        return "Authorization";
    }


    @Override
    public String getValue(Vault vault) {
        return vault.resolveSecret(passwordAlias);
    }
}
