package de.fraunhofer.iosb.app.controller.dto.auth;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;


public record NoAuthDTO() implements AuthenticationMethodDTO {

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
