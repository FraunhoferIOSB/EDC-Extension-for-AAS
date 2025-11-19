package de.fraunhofer.iosb.model.context.registry;

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.model.context.AasServerContext;

import java.net.URI;
import java.util.Objects;


public class AasRegistryContext extends AasServerContext {

    private final AuthenticationMethod authenticationMethod;
    private final boolean allowSelfSigned;


    private AasRegistryContext(URI uri, AuthenticationMethod authenticationMethod, boolean allowSelfSigned) {
        super(uri);
        this.authenticationMethod = authenticationMethod;
        this.allowSelfSigned = allowSelfSigned;
    }


    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }


    public boolean allowSelfSigned() {
        return allowSelfSigned;
    }


    public static class Builder {
        private URI uri;
        private AuthenticationMethod authenticationMethod;
        private boolean allowSelfSigned;


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


        public Builder allowSelfSigned(boolean allowSelfSigned) {
            this.allowSelfSigned = allowSelfSigned;
            return this;
        }


        public AasRegistryContext build() {
            Objects.requireNonNull(uri, "Access URI must be non-null");
            authenticationMethod = Objects.requireNonNullElse(authenticationMethod, new NoAuth());

            return new AasRegistryContext(uri, authenticationMethod, allowSelfSigned);
        }
    }
}
