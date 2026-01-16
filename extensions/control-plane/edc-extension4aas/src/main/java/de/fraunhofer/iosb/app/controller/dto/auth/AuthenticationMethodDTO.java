package de.fraunhofer.iosb.app.controller.dto.auth;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.eclipse.edc.iam.oauth2.spi.client.Oauth2Client;
import org.eclipse.edc.spi.security.Vault;

import javax.annotation.Nonnull;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ApiKeyDTO.class, name = "api-key"),
        @JsonSubTypes.Type(value = BearerAuthDTO.class, name = "bearer"),
        @JsonSubTypes.Type(value = BasicAuthDTO.class, name = "basic"),
})
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
