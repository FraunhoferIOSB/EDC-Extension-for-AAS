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
package de.fraunhofer.iosb.model.aas.auth;

import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;

public abstract class AuthenticationMethod {

    /**
     * Get the header value to add to the request headers to communicate with the service.
     * Headers: [... , (getHeader().key, getHeader().value), ...]
     *
     * @return The header to place in the request in order to authenticate
     */
    public @Nullable Map.Entry<String, String> getHeader() {
        return new AbstractMap.SimpleEntry<>("Authorization", getValue());
    }

    protected abstract String getValue();
}
