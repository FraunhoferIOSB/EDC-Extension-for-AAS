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
package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.app.Logger;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

/**
 * Util; Transform similar objects into other frameworks' implementations
 */
public final class Transformer {

    private static final Logger LOGGER = Logger.getInstance();

    private Transformer() {
    }

    /*
     * Transform an okHttp client response to a jakarta response object.
     */
    public static Response okHttpResponseToJakartaResponse(okhttp3.Response response) {
        int statuscode = response.code();
        String body;
        try {
            body = response.body().string();
        } catch (IOException e) {
            LOGGER.error("Failed transforming HTTP Response", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(statuscode).entity(body).build();
    }
}
