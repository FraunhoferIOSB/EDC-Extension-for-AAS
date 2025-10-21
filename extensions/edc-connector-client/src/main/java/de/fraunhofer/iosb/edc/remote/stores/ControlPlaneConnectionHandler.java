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

import de.fraunhofer.iosb.aas.lib.auth.AuthenticationMethod;
import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.entity.Entity;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;

public abstract class ControlPlaneConnectionHandler<T extends Entity> {

    public static final String MESSAGE_CODE_TEMPLATE = "Message: %s; Status code: %d";
    public static final String NO_MESSAGE = "No message from control-plane.";

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

    protected Stream<T> queryEntities(QuerySpec spec, Class<T> clazz) {
        String querySpecString = codec.serialize(spec);

        var request = controlPlane.prepareRequest(HttpMethod.POST, "request", querySpecString);

        var responseJsonString = executeRequest(request)
                .orElse(failure -> {
                    reportError(failure);
                    return null;
                });

        if (responseJsonString == null || responseJsonString.isBlank()) {
            return Stream.of();
        }

        Result<List<T>> deserialized = codec.deserializeList(responseJsonString, clazz);

        if (deserialized.failed()) {
            monitor.warning(deserialized.getFailureDetail());
        }

        return deserialized.getContent().stream();
    }

    protected T findById(String entityId, Class<T> clazz) {
        var request = controlPlane.prepareRequest(HttpMethod.GET, entityId, null);

        var responseJsonOrNull = executeRequest(request)
                .orElse(failure -> {
                    if (NOT_FOUND != failure.getReason()) {
                        reportError(failure);
                    }
                    return null;
                });

        if (responseJsonOrNull == null) {
            return null;
        }

        Result<T> deserialized = codec.deserialize(responseJsonOrNull, clazz);

        if (deserialized.failed()) {
            monitor.severe(deserialized.getFailureDetail());
            return null;
        }

        return deserialized.getContent();
    }

    protected StoreResult<Void> createEntity(T entity) {
        var serialized = codec.serialize(entity);

        var request = controlPlane.prepareRequest(HttpMethod.POST, serialized);

        var response = executeRequest(request);

        if (response.failed()) {
            if (response.reason() == CONFLICT) {
                return StoreResult.alreadyExists(String.format(getExistsTemplate(), entity.getId()));
            }
            reportError(response.getFailure());
            return StoreResult.generalError(response.getFailureDetail());
        }

        return StoreResult.success();
    }

    protected StoreResult<T> deleteById(String entityId, Class<T> clazz) {
        // NOTE: since deleteById requires the deleted asset as return value and the mgmt-api does not return it, we have to get it first.
        var entity = this.findById(entityId, clazz);

        if (entity == null) {
            return StoreResult.notFound(String.format(getNotFoundTemplate(), entityId));
        }

        // Send request
        var request = controlPlane.prepareRequest(HttpMethod.DELETE, entityId, null);
        // Deserialize response
        var response = executeRequest(request);

        if (!response.succeeded()) {
            if (NOT_FOUND == response.reason()) {
                return StoreResult.notFound(response.getFailureDetail());
            } else if (CONFLICT == response.reason()) {
                return StoreResult.alreadyLeased(response.getFailureDetail());
            }
            reportError(response.getFailure());
            return StoreResult.generalError(response.getFailureDetail());
        }

        return StoreResult.success(entity);
    }

    protected StoreResult<T> updateEntity(T entity, Class<T> clazz) {
        var entityString = codec.serialize(entity);

        var request = controlPlane.prepareRequest(HttpMethod.PUT, entityString);

        var response = executeRequest(request);

        if (!response.succeeded()) {

            if (NOT_FOUND == response.reason()) {
                return StoreResult.notFound(String.format(getNotFoundTemplate(), entity.getId()));
            }
            reportError(response.getFailure());
            return StoreResult.generalError(response.getFailureDetail());
        }

        return StoreResult.success(findById(entity.getId(), clazz));
    }

    protected abstract String getExistsTemplate();

    protected abstract String getNotFoundTemplate();

    protected ServiceResult<String> executeRequest(Request request) {
        try (Response response = this.httpClient.execute(request)) {

            ResponseBody body = response.body();
            if (!response.isSuccessful()) {
                // User errors: 404, 409, 403, 400

                String responseMessage = body.string();

                if (responseMessage.isBlank()) {
                    responseMessage = NO_MESSAGE;
                }

                int responseCode = response.code();
                return switch (responseCode) {
                    case 400 -> ServiceResult.badRequest(responseMessage);
                    case 401, 403, 407 -> ServiceResult.unauthorized(String.format(MESSAGE_CODE_TEMPLATE, responseMessage, responseCode));
                    case 404 -> ServiceResult.notFound(responseMessage);
                    case 409 -> ServiceResult.conflict(responseMessage);
                    default -> ServiceResult.unexpected(String.format(MESSAGE_CODE_TEMPLATE, responseMessage, responseCode));
                };
            }
            return ServiceResult.success(body.string());

        } catch (IOException controlPlaneConnectionException) {
            return ServiceResult.unexpected(controlPlaneConnectionException.getMessage());
        }
    }

    private void reportError(ServiceFailure failure) {
        monitor.severe(String.format("%s: %s", failure.getReason(), failure.getFailureDetail()));
    }

    public abstract static class Builder<T extends ControlPlaneConnectionHandler, B extends Builder<T, B>> {
        protected EdcHttpClient httpClient;
        protected Monitor monitor;
        protected String managementUri;
        protected String resourceName;
        private AuthenticationMethod authenticationMethod;
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

        public B managementUri(String managementUri) {
            this.managementUri = managementUri;
            return self();
        }

        public B authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return self();
        }

        public T build() {
            Objects.requireNonNull(httpClient);
            Objects.requireNonNull(monitor);
            Objects.requireNonNull(codec);
            Objects.requireNonNull(managementUri);
            Objects.requireNonNull(authenticationMethod);

            ControlPlaneConnection connection = new ControlPlaneConnection(URI.create(managementUri), resourceName, authenticationMethod);

            return create(monitor, httpClient, codec, connection);
        }

    }
}
