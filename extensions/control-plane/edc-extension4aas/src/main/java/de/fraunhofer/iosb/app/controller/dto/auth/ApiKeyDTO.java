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
