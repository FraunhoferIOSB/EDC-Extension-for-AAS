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

import com.fasterxml.jackson.annotation.JsonProperty;
import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.aas.lib.auth.impl.NoAuth;
import de.fraunhofer.iosb.aas.lib.net.AasAccessUrl;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static de.fraunhofer.iosb.aas.lib.type.AasConstants.AAS_V30_NAMESPACE;

public abstract class AasProvider {

    public static final String AAS_PROVIDER_TYPE = AAS_V30_NAMESPACE + "AasProvider";
    public static final String AAS_PROVIDER_AUTH = AAS_V30_NAMESPACE + "auth";
    public static final String AAS_PROVIDER_URL = AAS_V30_NAMESPACE + "url";
    protected final AasAccessUrl url;

    @JsonProperty(AAS_PROVIDER_AUTH)
    protected final AuthenticationMethod auth;

    public AasProvider(AasAccessUrl url, AuthenticationMethod auth) {
        this.url = Objects.requireNonNull(url);
        this.auth = Objects.requireNonNull(auth);
    }

    public AasProvider(AasAccessUrl url) {
        this(url, new NoAuth());
    }

    protected AasProvider(AasProvider from) {
        this(from.url, from.auth);
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
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(url);
    }

    @JsonProperty(AAS_PROVIDER_URL)
    public URL baseUrl() {
        return url.url();
    }

    public static abstract class Builder<B extends Builder<B>> {
        protected AasAccessUrl url;
        protected AuthenticationMethod authentication = new NoAuth();
        protected List<PolicyBinding> policyBindings = null;

        public B withPolicyBindings(List<PolicyBinding> policyBindings) {
            this.policyBindings = policyBindings;
            return self();
        }

        public B withUrl(URL url) {
            this.url = new AasAccessUrl(url);
            return self();
        }

        public B aasAccessUrl(AasAccessUrl url) {
            this.url = url;
            return self();
        }

        public B withAuth(AuthenticationMethod authentication) {
            this.authentication = Objects.requireNonNull(authentication);
            return self();
        }

        public B from(AasProvider from) {
            this.url = from.url;
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
