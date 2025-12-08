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
