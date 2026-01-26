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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.spi.security.Vault;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;


/**
 * Api key authentication: (key, value). Example: (x-api-key,password)
 */
public class ApiKey extends AuthenticationMethod {

    private final String keyName;
    private final Function<Vault, String> keyValueAlias;


    public ApiKey(@JsonProperty("keyName") String keyName, @JsonProperty("keyValue") String keyValue, Vault vault) {
        this.keyName = Objects.requireNonNull(keyName);
        this.keyValueAlias = getResolver(vault, keyValue);
    }


    @JsonCreator
    public ApiKey(@JsonProperty("keyName") String keyName, @JsonProperty("keyValueAlias") String keyValueAlias) {
        this.keyName = Objects.requireNonNull(keyName);

        Objects.requireNonNull(keyValueAlias);
        this.keyValueAlias = (v) -> v.resolveSecret(keyValueAlias);
    }


    @Override
    public Map.Entry<String, String> getHeader(Vault vault) {
        return new AbstractMap.SimpleEntry<>(keyName, getValue(vault));
    }


    public String getValue(Vault vault) {
        return keyValueAlias.apply(vault);
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor(Vault vault) {
        return HttpClient.newBuilder().authenticator(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(keyName, getValue(vault).toCharArray());
            }
        });
    }
}
