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
package de.fraunhofer.iosb.app.controller.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;

import javax.annotation.Nonnull;


/**
 * DTO representing an authentication method used to access a remote AAS server. Implementations carry the required
 * secrets and provide factory methods to build the corresponding {@link AuthenticationMethod}, optionally resolving
 * secrets from a vault and tokens from an OAuth2 client.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApiKeyDTO.class, name = "api-key"),
        @JsonSubTypes.Type(value = BearerAuthDTO.class, name = "bearer"),
        @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "basic"),
})
public interface AuthenticationMethodDTO {

    /**
     * Indicates whether this authentication method requires an OAuth2 client to obtain tokens.
     *
     * @return {@code true} if an OAuth2 client is required, {@code false} otherwise.
     */
    default boolean requiresOauth2Client() {
        return false;
    }


    /**
     * Indicates whether this authentication method requires a vault to resolve secrets.
     *
     * @return {@code true} if a vault is required, {@code false} otherwise.
     */
    default boolean requiresVault() {
        return true;
    }


    /**
     * Builds the authentication method using the given vault and OAuth2 client.
     *
     * @param vault The vault where secrets related to access the AAS server may be stored.
     * @param client The OAuth2 client used to obtain tokens, if necessary.
     * @return The resolved authentication method.
     */
    @Nonnull
    AuthenticationMethod asAuth(@Nonnull Vault vault, @Nonnull Oauth2Client client);


    /**
     * Builds the authentication method using the given vault.
     *
     * @param vault The vault where secrets related to access the AAS server may be stored.
     * @return The resolved authentication method.
     */
    @Nonnull
    AuthenticationMethod asAuth(@Nonnull Vault vault);


    /**
     * Builds the authentication method without any vault or OAuth2 client. Only valid for authentication methods that
     * do not require secrets (e.g. {@link NoAuthDTO}).
     *
     * @return The resolved authentication method.
     */
    @Nonnull
    AuthenticationMethod asAuth();
}
