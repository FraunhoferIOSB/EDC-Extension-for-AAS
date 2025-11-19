package de.fraunhofer.iosb.app.controller.dto;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;

import java.net.URI;
import java.util.List;
import java.util.Objects;


/**
 * DTO containing information to register a remote AAS repository.
 *
 * @param url URI to use to connect to the AAS repository, including any path prefixes (e.g., /api/v3.0)
 * @param auth The authentication method used to communicate with the registry.
 * @param policyBindings List of {@link PolicyBinding}. If defined, only elements referred by the policyBindings are registered (optional, default: no custom
 *         PolicyBindings, register all elements).
 */
public record RemoteAasRepositoryContextDTO(URI url, AuthenticationMethod auth, List<PolicyBinding> policyBindings) {
    public RemoteAasRepositoryContextDTO {
        Objects.requireNonNull(url, "'url' cannot be null!");
        auth = Objects.requireNonNullElse(auth, new NoAuth());
        policyBindings = Objects.requireNonNullElse(policyBindings, List.of());
    }


    public RemoteAasRepositoryContext asContext() {
        return new RemoteAasRepositoryContext.Builder()
                .uri(this.url())
                .policyBindings(this.policyBindings())
                .authenticationMethod(this.auth())
                .onlySubmodels(Configuration.getInstance().onlySubmodels())
                .allowSelfSigned(Configuration.getInstance().isAllowSelfSignedCertificates())
                .build();
    }
}
