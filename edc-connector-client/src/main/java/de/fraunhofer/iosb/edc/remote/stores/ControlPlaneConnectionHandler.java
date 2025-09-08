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
package de.fraunhofer.iosb.edc.remote.stores;

import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.ServiceResult;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public abstract class ControlPlaneConnectionHandler {

    public static final String UNEXPECTED_ERROR = "Unexpected error from (remote) control-plane, %s: %s";
    public static final String MALFORMED_ERROR = "MalformedRequestError from (remote) control-plane, %s: %s";
    public static final String NO_MESSAGE = "No message.";

    protected final ControlPlaneConnection controlPlane;
    protected final EdcHttpClient httpClient;
    protected final Monitor monitor;
    protected final Codec codec;

    public ControlPlaneConnectionHandler(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.codec = codec;
        this.controlPlane = connection;
    }

    protected ServiceResult<String> executeRequest(Request request) {
        try (Response response = this.httpClient.execute(request)) {

            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                // User error: 404, 409
                return switch (response.code()) {
                    case 404 -> ServiceResult.notFound(response.body() != null ? response.body().string() : NO_MESSAGE);
                    case 409 -> ServiceResult.conflict(response.body() != null ? response.body().string() : NO_MESSAGE);
                    case 400 -> throw new EdcException(String.format(MALFORMED_ERROR, response.message(),
                            response.body() != null ? response.body().string() : NO_MESSAGE));
                    default -> throw new EdcException(String.format(UNEXPECTED_ERROR, response.message(),
                            response.body() != null ? response.body().string() : NO_MESSAGE));
                };
            }
            return ServiceResult.success(body != null ? body.string() : NO_MESSAGE);

        } catch (IOException controlPlaneConnectionException) {
            return ServiceResult.unexpected(controlPlaneConnectionException.getMessage());
        }
    }

    public static abstract class Builder<T extends ControlPlaneConnectionHandler, B extends Builder<T, B>> {
        protected EdcHttpClient httpClient;
        protected Monitor monitor;
        protected URI managementUri;
        protected String resourceName;
        private String apiKey;
        private Codec codec;

        protected abstract B self();

        protected abstract T create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection);

        public B monitor(Monitor v) {
            this.monitor = v;
            return self();
        }

        public B codec(Codec codec) {
            this.codec = codec;
            return self();
        }

        public B httpClient(EdcHttpClient v) {
            this.httpClient = v;
            return self();
        }

        public B managementUri(URI managementUri) {
            this.managementUri = managementUri;
            return self();
        }

        public B apiKey(String apiKey) {
            this.apiKey = apiKey;
            return self();
        }

        public T build() {
            Objects.requireNonNull(httpClient, "httpClient must not be null");
            Objects.requireNonNull(monitor, "monitor must not be null");
            Objects.requireNonNull(codec, "codec must not be null");
            Objects.requireNonNull(resourceName, "resourceName must not be null");

            ControlPlaneConnection connection;
            if (apiKey != null) {
                connection = new ControlPlaneConnection(managementUri, resourceName, apiKey);
            } else {
                connection = new ControlPlaneConnection(managementUri, resourceName);
            }

            return create(monitor, httpClient, codec, connection);
        }
    }
}
