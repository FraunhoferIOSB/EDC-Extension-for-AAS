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
import de.fraunhofer.iosb.aas.lib.auth.impl.BasicAuth;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;


/**
 * Carries authentication secrets used to authenticate via basic auth.
 *
 * @param username Username for basic auth. Will be stored in vault when creating {@link BasicAuth}.
 * @param password Password for basic auth. Will be stored in vault when creating {@link BasicAuth}.
 */
public record BasicAuthDTO(String username, String password) implements AuthenticationMethodDTO {

    @Override
    public @NotNull AuthenticationMethod asAuth(@NotNull Vault vault, @NotNull Oauth2Client client) {
        return new BasicAuth(username, password, vault);
    }


    @Override
    public @NotNull AuthenticationMethod asAuth(@NotNull Vault vault) {
        return new BasicAuth(username, password, vault);
    }


    @Override
    public @NotNull AuthenticationMethod asAuth() {
        throw new IllegalArgumentException("basic auth requires vault");
    }
}
