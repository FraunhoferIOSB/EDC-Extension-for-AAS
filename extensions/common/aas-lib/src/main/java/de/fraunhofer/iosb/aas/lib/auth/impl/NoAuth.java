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
package de.fraunhofer.iosb.aas.lib.auth.impl;


import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpClient;
import java.util.Map;

public class NoAuth extends AuthenticationMethod {

    public NoAuth() {
    }

    @Override
    public @Nullable Map.Entry<String, String> getHeader() {
        return null;
    }

    @Override
    protected @Nullable String getValue() {
        return null;
    }


    @Override
    public HttpClient.Builder httpClientBuilderFor() {
        return HttpClient.newBuilder();
    }
}
