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
package de.fraunhofer.iosb.client;

import de.fraunhofer.iosb.aas.lib.model.PolicyBinding;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.net.URI;
import java.util.List;
import java.util.Map;

public interface AasServerClient {

    boolean isAvailable();

    URI getUri();

    default List<Reference> getReferences() {
        return List.of();
    }

    default List<PolicyBinding> getPolicyBindings() {
        return List.of();
    }

    default boolean requiresAuthentication() {
        return false;
    }

    default Map<String, String> getHeaders() {
        return Map.of();
    }
}
