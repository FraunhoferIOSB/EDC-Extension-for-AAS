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


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApiKeyDTO.class, name = "api-key"),
        @JsonSubTypes.Type(value = BearerAuthDTO.class, name = "bearer"),
        @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "basic"),
})
public interface AuthenticationMethodDTO {

    default boolean requiresOauth2Client() {
        return false;
    }


    default boolean requiresVault() {
        return true;
    }


    @Nonnull
    AuthenticationMethod asAuth(@Nonnull Vault vault, @Nonnull Oauth2Client client);


    @Nonnull
    AuthenticationMethod asAuth(@Nonnull Vault vault);


    @Nonnull
    AuthenticationMethod asAuth();
}
