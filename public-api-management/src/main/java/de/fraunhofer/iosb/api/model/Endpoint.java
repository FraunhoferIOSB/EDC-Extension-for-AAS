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
package de.fraunhofer.iosb.api.model;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Record of an endpoint.
 *
 * @param suffix        The relevant suffix of the endpoint (i.e. the path of the URL).
 * @param method        The method through which the endpoint can be accessed.
 * @param customHeaders Custom headers like special authentication keys can be passed here. This is a multivalued map
 */
public record Endpoint(String suffix, HttpMethod method, Map<String, List<String>> customHeaders) {

    public Endpoint {
        Objects.requireNonNull(suffix);
        Objects.requireNonNull(method);
        Objects.requireNonNull(customHeaders);
    }

    /**
     * Check whether this endpoint instance is covered by the other endpoint.
     * This means, suffix and  method have to match.
     * Headers of this endpoint have to be within other's headers.
     * This is to check if the other endpoint contains custom needed headers like additional api keys.
     *
     * @param other Other endpoint, whose headers must contain this endpoint's headers, as well as match suffix and method.
     * @return True if the condition holds, else false.
     */
    public boolean isCoveredBy(Endpoint other) {
        if (!this.suffix().equals(other.suffix()) || !this.method().equals(other.method())) {
            return false;
        }

        return this.customHeaders().entrySet().stream().allMatch(entry ->
                other.customHeaders().containsKey(entry.getKey()) &&
                        new HashSet<>(other.customHeaders().get(entry.getKey())).containsAll(entry.getValue()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Endpoint endpoint = (Endpoint) o;
        return Objects.equals(suffix, endpoint.suffix) && method == endpoint.method && Objects.equals(customHeaders, endpoint.customHeaders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(suffix, method, customHeaders);
    }
}
