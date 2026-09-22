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

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.util.Objects;


/**
 * Api key authentication: (key, value). Example: (x-api-key,password)
 */
public class ApiKey extends AuthenticationMethod {

    private final String keyName;
    private final String keyValueAlias;


    /**
     * Creates a new ApiKey authentication with a direct key value.
     *
     * @param keyName the header key name.
     * @param keyValue the header key value.
     * @param vault the vault to store the key value in.
     */
    public ApiKey(String keyName, String keyValue, Vault vault) {
        this.keyName = Objects.requireNonNull(keyName);
        this.keyValueAlias = store(vault, keyValue);
    }


    /**
     * Creates a new ApiKey authentication with a vault alias for the key value.
     *
     * @param keyName the header key name.
     * @param keyValueAlias the vault alias for the key value.
     */
    public ApiKey(String keyName, String keyValueAlias) {
        this.keyName = Objects.requireNonNull(keyName);
        this.keyValueAlias = Objects.requireNonNull(keyValueAlias);
    }


    @Override
    public void decorate(HttpDataAddress.Builder addressBuilder) {
        addressBuilder.authKey(keyName);
        addressBuilder.secretName(keyValueAlias);
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor(Vault vault) {
        return HttpClient.newBuilder().authenticator(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(keyName, getValue(vault).toCharArray());
            }
        });
    }


    @Override
    public String getKey() {
        return keyName;
    }


    @Override
    public String getValue(Vault vault) {
        return vault.resolveSecret(keyValueAlias);
    }
}
