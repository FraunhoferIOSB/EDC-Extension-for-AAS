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
package de.fraunhofer.iosb.ilt.dataspace.model.context;

import de.fraunhofer.iosb.ilt.dataspace.aas.lib.auth.AuthenticationMethod;

import java.net.URI;


/**
 * Client-specific view of a context
 */
public interface RemoteClientContext {

    /**
     * Returns the URI of the remote server.
     *
     * @return the server URI.
     */
    URI getUri();


    /**
     * Returns the authentication method for connecting to the remote server.
     *
     * @return the authentication method.
     */
    AuthenticationMethod getAuthenticationMethod();


    /**
     * Returns whether self-signed certificates are allowed when connecting to the remote server.
     *
     * @return true if self-signed certificates are allowed, else false.
     */
    boolean allowSelfSigned();
}
