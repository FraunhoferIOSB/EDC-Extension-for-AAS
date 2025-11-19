package de.fraunhofer.iosb.model.context.registry;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.model.context.AasServerContext;

import java.net.URI;
import java.util.Objects;


public class AasRegistryContext extends AasServerContext {
    private final AuthenticationMethod authenticationMethod;


    private AasRegistryContext(URI uri, AuthenticationMethod authenticationMethod) {
        super(uri);
        this.authenticationMethod = authenticationMethod;
    }


    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    public static class Builder {
        private URI uri;
        private AuthenticationMethod authenticationMethod;


        public Builder() {
        }


        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }


        public Builder authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return this;
        }


        public AasRegistryContext build() {
            Objects.requireNonNull(uri, "FA³ST MessageBus cannot be null");
            authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());

            return new AasRegistryContext(uri, authenticationMethod);
        }
    }
}
