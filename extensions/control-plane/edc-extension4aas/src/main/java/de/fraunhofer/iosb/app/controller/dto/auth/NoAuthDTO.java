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
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;


/**
 * DTO representing the absence of authentication, i.e. no credentials are required to access the AAS server.
 */
public record NoAuthDTO() implements AuthenticationMethodDTO {

    /**
     * Indicates that no vault is required since no secrets need to be resolved.
     *
     * @return {@code false}.
     */
    public boolean requiresVault() {
        return false;
    }


    @Override
    public @Nonnull AuthenticationMethod asAuth(@NotNull Vault vault, @NotNull Oauth2Client client) {
        return new NoAuth();
    }


    @Override
    public @Nonnull AuthenticationMethod asAuth(@NotNull Vault vault) {
        return new NoAuth();
    }


    @Override
    public @Nonnull AuthenticationMethod asAuth() {
        return new NoAuth();
    }
}
