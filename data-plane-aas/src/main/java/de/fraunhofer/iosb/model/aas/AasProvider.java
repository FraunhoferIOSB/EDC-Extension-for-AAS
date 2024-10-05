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
package de.fraunhofer.iosb.model.aas;

import com.fasterxml.jackson.annotation.JsonAlias;
import de.fraunhofer.iosb.model.aas.auth.AuthenticationMethod;
import de.fraunhofer.iosb.model.aas.auth.impl.NoAuth;
import de.fraunhofer.iosb.model.aas.net.AasAccessUrl;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AasProvider {

    public static final String AAS_V3_PREFIX = "/api/v3.0";

    @JsonAlias("url")
    private final AasAccessUrl url;
    @JsonAlias("auth")
    private final AuthenticationMethod authentication;

    public AasProvider(AasAccessUrl url) {
        this.url = url;
        this.authentication = new NoAuth();
    }

    public AasProvider(AasAccessUrl url, AuthenticationMethod authentication) {
        this.url = url;
        this.authentication = authentication;
    }

    protected AasProvider(AasProvider from) {
        this.url = from.url;
        this.authentication = from.authentication;
    }

    public Map<String, String> getHeaders() {
        var header = authentication.getHeader();
        var responseMap = new HashMap<String, String>();
        if (header != null) {
            responseMap.put(header.getKey(), header.getValue());
        }

        return responseMap;
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

    public URL getAccessUrl() {
        return url.url();
    }

    public URL getAccessUrlV3() {
        return url.urlV3();
    }
}
