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
package de.fraunhofer.iosb.app.controller.dto;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.app.controller.dto.auth.AuthenticationMethodDTO;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;

import javax.annotation.Nullable;


public interface RemoteAasServerDTO {

    AuthenticationMethodDTO getAuthenticationMethodDTO();


    default AuthenticationMethod toAuthenticationMethod(@Nullable Vault vault, @Nullable Oauth2Client oauth2Client) {
        AuthenticationMethodDTO authenticationMethodDTO = getAuthenticationMethodDTO();

        if (authenticationMethodDTO.requiresVault()) {
            if (vault == null) {
                throw new IllegalArgumentException("Auth used without available vault");
            }
            if (authenticationMethodDTO.requiresOauth2Client()) {
                if (oauth2Client == null) {
                    throw new IllegalArgumentException("Bearer auth used without available oauth2 client");
                }

                return authenticationMethodDTO.asAuth(vault, oauth2Client);
            }

            return authenticationMethodDTO.asAuth(vault);
        }
        return authenticationMethodDTO.asAuth();
    }
}
