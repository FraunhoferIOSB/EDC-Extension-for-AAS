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
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class HttpRestClient {

    private final Logger logger;
    private final OkHttpClient client;

    public HttpRestClient(OkHttpClient client) {
        logger = Logger.getInstance();
        this.client = client;
    }

    /**
     * Issue a get request to a given url

     * @param url the url to where the get request goes
     * @return Response by the service behind the url
     */
    public Response get(URL url) throws IOException {
        logger.debug("GET " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .get()
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a put request to a given url

     * @param url     the url to where the put request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response put(URL url, String payload) throws IOException {
        logger.debug("PUT " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .put(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a post request to a given url

     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response post(URL url, String payload) throws IOException {
        logger.debug("POST " + url);
        var request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.get(url)))
                .post(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a delete request to a given url

     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response delete(URL url, String payload) throws IOException {
        logger.debug("DELETE " + url);

        Request request;
        if (Objects.nonNull(payload)) {
            RequestBody requestBody = RequestBody.create(payload, MediaType.parse("application/json"));
            request = new Request.Builder()
                    .url(Objects.requireNonNull(HttpUrl.get(url)))
                    .delete(requestBody)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(Objects.requireNonNull(HttpUrl.get(url)))
                    .delete()
                    .build();
        }
        return client.newCall(request).execute();
    }
}
