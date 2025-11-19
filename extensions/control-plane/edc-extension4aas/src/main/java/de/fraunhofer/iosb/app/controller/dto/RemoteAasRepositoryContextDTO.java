package de.fraunhofer.iosb.app.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import de.fraunhofer.iosb.ilt.faaast.service.util.Ensure;
import de.fraunhofer.iosb.model.context.repository.remote.RemoteAasRepositoryContext;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;


public record RemoteAasRepositoryContextDTO(
        @JsonAlias({
                AAS_V30_NAMESPACE + "url",
                "url"
        }) URI uri,
        @JsonAlias({
                AAS_V30_NAMESPACE + "auth",
                "auth"
        }) AuthenticationMethod authenticationMethod,
        @JsonAlias({
                AAS_V30_NAMESPACE + "policyBindings",
                "policyBindings"
        }) List<PolicyBinding> policyBindings) {
    public RemoteAasRepositoryContextDTO {
        Ensure.requireNonNull(uri, "'url' cannot be null!");
        authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());
        policyBindings = Objects.requireNonNullElse(policyBindings, List.of());
    }


    public RemoteAasRepositoryContext asContext() {
        return new RemoteAasRepositoryContext.Builder()
                .uri(this.uri())
                .policyBindings(this.policyBindings())
                .authenticationMethod(this.authenticationMethod())
                .build();
    }
}
