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
package de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.ApiKey;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.BasicAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.BearerAuth;
import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.impl.NoAuth;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.spi.security.Vault;

import java.net.http.HttpClient;
import java.util.UUID;


/**
 * Describes authentication methods for HTTP authentication, i.e. key-value pairs appended to the headers of an HTTP
 * request.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuth.class, name = "basic"),
        @JsonSubTypes.Type(value = ApiKey.class, name = "api-key"),
        @JsonSubTypes.Type(value = BearerAuth.class, name = "bearer"),
        @JsonSubTypes.Type(value = NoAuth.class)
})
public abstract class AuthenticationMethod {

    /** Default constructor. */
    protected AuthenticationMethod() {}


    /**
     * Decorates the given HTTP data address builder with the authentication-specific properties.
     *
     * @param addressBuilder the builder to decorate.
     */
    public abstract void decorate(HttpDataAddress.Builder addressBuilder);


    /**
     * Returns the HttpClient builder for this authentication method.
     *
     * @param vault Vault needed to retrieve secrets.
     * @return HttpClient.Builder for use in FA³ST client.
     */
    public abstract HttpClient.Builder httpClientBuilderFor(Vault vault);


    /**
     * Returns the header key to set for this authentication method.
     *
     * @return the header key.
     */
    public abstract String getKey();


    /**
     * Returns the header value for this authentication method, resolving any secrets from the vault.
     *
     * @param vault the vault to resolve secrets from.
     * @return the header value.
     */
    public abstract String getValue(Vault vault);


    /**
     * Stores a secret in the vault under a new random alias and returns the alias.
     *
     * @param vault the vault to store the secret in.
     * @param secret the secret value to store.
     * @return the alias under which the secret was stored.
     */
    protected String store(Vault vault, String secret) {
        String alias = UUID.randomUUID().toString();
        var storeResult = vault.storeSecret(alias, secret);
        if (storeResult.failed()) {
            throw new IllegalArgumentException(storeResult.getFailureDetail());
        }
        return alias;
    }

}
