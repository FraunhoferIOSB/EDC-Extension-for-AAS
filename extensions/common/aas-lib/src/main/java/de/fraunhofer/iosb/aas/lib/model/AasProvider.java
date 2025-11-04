/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.aas.lib.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.net.AasAccessUri;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.fraunhofer.iosb.constants.AasConstants.AAS_V30_NAMESPACE;

public abstract class AasProvider {

    public static final String AAS_PROVIDER_AUTH = AAS_V30_NAMESPACE + "auth";
    public static final String AAS_PROVIDER_URL = AAS_V30_NAMESPACE + "uri";
    protected final AasAccessUri uri;

    @JsonProperty(AAS_PROVIDER_AUTH)
    protected final AuthenticationMethod auth;

    public AasProvider(AasAccessUri uri, AuthenticationMethod auth) {
        this.uri = Objects.requireNonNull(uri);
        this.auth = Objects.requireNonNull(auth);
    }

    public AasProvider(AasAccessUri uri) {
        this(uri, new NoAuth());
    }

    protected AasProvider(AasProvider from) {
        this(from.uri, from.auth);
    }

    public Map<String, String> getHeaders() {
        var header = auth.getHeader();
        var responseMap = new HashMap<String, String>();
        if (header != null) {
            responseMap.put(header.getKey(), header.getValue());
        }

        return responseMap;
    }

    @JsonProperty(AAS_PROVIDER_AUTH)
    public AuthenticationMethod getAuth() {
        return auth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AasProvider that = (AasProvider) o;
        return Objects.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    @JsonProperty(AAS_PROVIDER_URL)
    public URI baseUri() {
        return uri.uri();
    }

    public abstract static class Builder<B extends Builder<B>> {
        protected AasAccessUri uri;
        protected AuthenticationMethod authentication = new NoAuth();
        protected List<PolicyBinding> policyBindings = null;

        public B withPolicyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
            return self();
        }

        @JsonAlias("url")
        public B withUri(URI uri) {
            this.uri = new AasAccessUri(uri);
            return self();
        }

        public B aasAccessUri(AasAccessUri url) {
            this.uri = url;
            return self();
        }

        public B withAuth(AuthenticationMethod authentication) {
            this.authentication = Objects.requireNonNull(authentication);
            return self();
        }

        public B from(AasProvider from) {
            this.uri = from.uri;
            this.authentication = from.auth;
            return self();
        }

        public abstract AasProvider build();

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }
    }
}
