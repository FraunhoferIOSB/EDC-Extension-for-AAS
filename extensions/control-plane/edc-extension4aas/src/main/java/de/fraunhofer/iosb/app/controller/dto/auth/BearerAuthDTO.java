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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.BearerAuth;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;
import javax.annotation.Nonnull;


/**
 * Carries authentication secrets used to create a bearer token from an IdP.
 *
 * @param uri URI of the IdP.
 * @param clientId Client ID. Will be stored in vault when creating {@link BearerAuth}.
 * @param clientSecret Client secret. Will be stored in vault when creating {@link BearerAuth}.
 * @param username Username. Will be stored in vault when creating {@link BearerAuth}.
 * @param password Password. Will be stored in vault when creating {@link BearerAuth}.
 */
public record BearerAuthDTO(URI uri, String clientId, String clientSecret, String username, String password) implements AuthenticationMethodDTO {
    public BearerAuthDTO {
        Objects.requireNonNull(clientId);
        Objects.requireNonNull(clientSecret);
    }


    @Override
    public boolean requiresOauth2Client() {
        return true;
    }


    public @Nonnull AuthenticationMethod asAuth(@NotNull Vault vault, @NotNull Oauth2Client client) {
        return new BearerAuth(clientId, clientSecret, username, password, uri, client, vault);
    }


    public @Nonnull AuthenticationMethod asAuth(@NotNull Vault vault) {
        throw new IllegalArgumentException("Bearer auth requires oauth2 client");
    }


    @Override
    public @Nonnull AuthenticationMethod asAuth() {
        throw new IllegalArgumentException("Bearer auth requires vault, oauth2 client");
    }
}
