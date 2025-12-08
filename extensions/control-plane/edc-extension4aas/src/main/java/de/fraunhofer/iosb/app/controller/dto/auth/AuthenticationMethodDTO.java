package de.fraunhofer.iosb.app.controller.dto.auth;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;

import javax.annotation.Nonnull;


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
