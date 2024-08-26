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
package de.fraunhofer.iosb.model.aas.auth.impl;

import de.fraunhofer.iosb.model.aas.auth.AuthenticationMethod;

import java.util.Base64;

/**
 * <a href="https://datatracker.ietf.org/doc/html/rfc7617#section-2">rfc7617</a>
 */
public class BasicAuth extends AuthenticationMethod {

    private final Base64.Encoder encoder = Base64.getEncoder();

    private final String username;
    private final String password;

    public BasicAuth(String username, String password) {
        this.username = username;
        this.password = password;
    }

    protected String getValue() {
        return "Basic %s".formatted(encoder.encodeToString("%s:%s".formatted(username, password).getBytes()));
    }
}
