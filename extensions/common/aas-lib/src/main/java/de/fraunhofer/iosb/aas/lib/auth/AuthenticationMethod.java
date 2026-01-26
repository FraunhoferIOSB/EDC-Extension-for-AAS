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
package de.fraunhofer.iosb.aas.lib.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.aas.lib.auth.impl.BasicAuth;
import de.fraunhofer.iosb.aas.lib.auth.impl.BearerAuth;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import org.eclipse.edc.spi.security.Vault;

import java.net.http.HttpClient;
import java.util.AbstractMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;


/**
 * Describes authentication methods for HTTP authentication, i.e. key-value pairs appended to the headers of an HTTP request.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuth.class, name = "basic"),
        @JsonSubTypes.Type(value = ApiKey.class, name = "api-key"),
        @JsonSubTypes.Type(value = BearerAuth.class, name = "bearer"),
        @JsonSubTypes.Type(value = NoAuth.class)
})
public abstract class AuthenticationMethod {

    /**
     * Get the header value to add to the request headers to communicate with the service. Headers: [... , (getHeader().key, getHeader().value), ...] The secrets needed to produce
     * the header value are resolved from the vault.
     *
     * @return The header to place in the request in order to authenticate
     */
    public Map.Entry<String, String> getHeader(Vault vault) {
        return new AbstractMap.SimpleEntry<>("Authorization", getValue(vault));
    }


    /**
     * Get HttpClient builder for this authentication method.
     *
     * @param vault Vault needed to retrieve secrets.
     * @return HttpClient.Builder for use in FA³ST client.
     */
    public abstract HttpClient.Builder httpClientBuilderFor(Vault vault);


    /**
     * Get the value of the authorization header.
     *
     * @param vault Vault to retrieve secrets from.
     * @return The value of the authorization header
     */
    public abstract String getValue(Vault vault);


    protected Function<Vault, String> getResolver(Vault vault, String secret) {
        if (secret == null) {
            return (v) -> null;
        }
        String alias = store(vault, secret);
        return (Vault v) -> v.resolveSecret(alias);
    }


    private String store(Vault vault, String secret) {
        String alias = UUID.randomUUID().toString();
        var storeResult = vault.storeSecret(alias, secret);
        if (storeResult.failed()) {
            throw new IllegalArgumentException(storeResult.getFailureDetail());
        }
        return alias;
    }

}
