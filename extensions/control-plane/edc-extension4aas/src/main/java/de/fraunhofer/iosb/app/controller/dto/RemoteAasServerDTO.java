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
