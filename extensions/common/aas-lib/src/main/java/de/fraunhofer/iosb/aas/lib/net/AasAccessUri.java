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
package de.fraunhofer.iosb.aas.lib.net;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.Objects;


/**
 * The part of the AAS access URL to directly access the /submodel /shell and /concept-description endpoints, i.e. "http://aas-service:port/path/to".
 * <p>An example could be "https://localhost:8080/api/v3.0"
 * <p>URL wrapper with equals method appropriate for AAS service access URLs
 */
public record AasAccessUri(@NotNull URI uri) {

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AasAccessUri that = (AasAccessUri) o;
        return Objects.equals(uri, that.uri);
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }


    @Override
    public @NotNull String toString() {
        return uri.toString();
    }
}
