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
package de.fraunhofer.iosb.model.auth.impl;

import de.fraunhofer.iosb.model.auth.AuthenticationMethod;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;

/**
 * Api key authentication: (key, value).
 * Example: (x-api-key,password)
 */
public class ApiKey extends AuthenticationMethod {

    private final String key;
    private final String keyValue;

    public ApiKey(String key, String keyValue) {
        this.key = key;
        this.keyValue = keyValue;
    }

    @Override
    public @Nullable Map.Entry<String, String> getHeader() {
        return new AbstractMap.SimpleEntry<>(key, getValue());
    }

    @Override
    protected String getValue() {
        return keyValue;
    }
}
