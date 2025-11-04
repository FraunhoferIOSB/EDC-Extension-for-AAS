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
import org.eclipse.edc.spi.security.Vault;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.util.AbstractMap;
import java.util.Map;

/**
 * Get vault secret for authentication with x-api-key
 */
public class VaultAuth extends AuthenticationMethod {

    private static final String KEY = "x-api-key";

    private final Vault vault;
    private final String alias;

    public VaultAuth(Vault vault, String alias) {
        this.vault = vault;
        this.alias = alias;
    }

    @Override
    public Map.Entry<String, String> getHeader() {
        return new AbstractMap.SimpleEntry<>(KEY, getValue());
    }

    @Override
    protected String getValue() {
        return vault.resolveSecret(alias);
    }

    @Override
    public HttpClient.Builder httpClientBuilderFor() {
        // TODO
        return HttpClient.newBuilder().authenticator(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(KEY, getValue().toCharArray());
            }
        });
    }
}
