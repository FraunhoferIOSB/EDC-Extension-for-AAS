package de.fraunhofer.iosb.app.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.ilt.faaast.service.util.Ensure;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;

import java.net.URI;
import java.util.Objects;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


public record AasRegistryContextDTO(
        @JsonAlias({
                AAS_V30_NAMESPACE + "url",
                "url"
        }) URI uri,
        @JsonAlias({
                AAS_V30_NAMESPACE + "auth",
                "auth"
        }) AuthenticationMethod authenticationMethod) {
    public AasRegistryContextDTO {
        Ensure.requireNonNull(uri, "'url' cannot be null!");
        authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());
    }


    public AasRegistryContext asContext() {
        return new AasRegistryContext.Builder()
                .uri(this.uri())
                .authenticationMethod(this.authenticationMethod())
                .allowSelfSigned(Configuration.getInstance().isAllowSelfSignedCertificates())
                .build();
    }
}
