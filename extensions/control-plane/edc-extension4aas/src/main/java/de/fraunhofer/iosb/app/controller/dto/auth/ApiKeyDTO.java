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
import de.fraunhofer.iosb.aas.lib.auth.impl.ApiKey;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;


/**
 * Carries auth secrets for an api key authentication mechanism.
 *
 * @param keyName The key name. E.g., x-api-key
 * @param keyValue The key value. Will be stored in vault when creating {@link ApiKey}.
 */
public record ApiKeyDTO(String keyName, String keyValue) implements AuthenticationMethodDTO {

    @Override
    public @NotNull AuthenticationMethod asAuth(@NotNull Vault vault, @NotNull Oauth2Client client) {
        return new ApiKey(keyName, keyValue, vault);
    }


    @Override
    public @NotNull AuthenticationMethod asAuth(@NotNull Vault vault) {
        return new ApiKey(keyName, keyValue, vault);
    }


    @Override
    public @NotNull AuthenticationMethod asAuth() {
        throw new IllegalArgumentException("Api-Key auth requires vault");
    }
}
