package de.fraunhofer.iosb.app.controller.dto;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.model.context.registry.AasRegistryContext;

import java.net.URI;
import java.util.Objects;


/**
 * DTO containing information to register a remote AAS registry.
 *
 * @param uri URI to use to connect to the AAS registry, including any path prefixes (e.g., /api/v3.0)
 * @param auth The authentication method used to communicate with the registry.
 */
public record AasRegistryContextDTO(URI uri, AuthenticationMethod auth) {
    public AasRegistryContextDTO {
        Objects.requireNonNull(uri, "'url' cannot be null!");
        auth = Objects.requireNonNullElse(auth, new NoAuth());
    }


    public AasRegistryContext asContext() {
        return new AasRegistryContext.Builder()
                .uri(this.uri())
                .authenticationMethod(this.auth())
                .allowSelfSigned(Configuration.getInstance().isAllowSelfSignedCertificates())
                .build();
    }
}
