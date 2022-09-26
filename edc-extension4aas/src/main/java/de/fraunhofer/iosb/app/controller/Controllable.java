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
package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.RequestType;
import jakarta.ws.rs.core.Response;

import java.net.URL;

/**
 * A controller interface.
 */
public interface Controllable {

    /**
     * Handles a request of type {@link RequestType}.

     * @param requestType The request type
     * @param requestData String array containing the request body as first and
     *                    request parameters as second element.
     * @return A response object according to {@link jakarta.ws.rs.core.Response
     *         Response}
     */
    Response handleRequest(RequestType requestType, URL url, String... requestData);

}
