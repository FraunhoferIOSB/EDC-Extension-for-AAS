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
import de.fraunhofer.iosb.codec.Codec;
import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.participantcontext.spi.types.AbstractParticipantResource;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.spi.security.Vault;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.eclipse.edc.spi.result.ServiceFailure.Reason.CONFLICT;
import static org.eclipse.edc.spi.result.ServiceFailure.Reason.NOT_FOUND;


/**
 * Abstract base handler that interacts with the EDC control plane via HTTP for participant resources.
 *
 * @param <T> the participant resource type handled by this handler.
 */
public abstract class ControlPlaneConnectionHandler<T extends AbstractParticipantResource> {

    private static final String MESSAGE_CODE_TEMPLATE = "Message: %s; Status code: %d";
    private static final String NO_MESSAGE = "No message from control-plane.";

    private final ControlPlaneConnection controlPlane;
    private final EdcHttpClient httpClient;
    private final Monitor monitor;
    private final Codec codec;


    /**
     * Creates a new control-plane connection handler.
     *
     * @param monitor the monitor used for logging.
     * @param httpClient the HTTP client used to execute requests.
     * @param codec the codec used to serialize and deserialize entities.
     * @param connection the control-plane connection used to build requests.
     */
    public ControlPlaneConnectionHandler(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        this.monitor = monitor;
        this.httpClient = httpClient;
        this.codec = codec;
        this.controlPlane = connection;
    }


    /**
     * Queries entities from the control plane matching the given query specification.
     *
     * @param spec the query specification to serialize and send.
     * @param clazz the target entity type.
     * @return a stream of matching entities, or an empty stream on failure.
     */
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


    /**
     * Retrieves an entity by its identifier from the control plane.
     *
     * @param entityId the identifier of the entity to retrieve.
     * @param clazz the target entity type.
     * @return the entity, or {@code null} if it is not found or deserialization fails.
     */
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


    /**
     * Creates an entity on the control plane.
     *
     * @param entity the entity to create.
     * @return a store result indicating success, already-exists, or a general error.
     */
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


    /**
     * Deletes an entity by its identifier from the control plane.
     *
     * @param entityId the identifier of the entity to delete.
     * @param clazz the target entity type.
     * @return a store result with the deleted entity, or a not-found, already-leased, or general error.
     */
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
            }
            else if (CONFLICT == response.reason()) {
                return StoreResult.alreadyLeased(response.getFailureDetail());
            }
            reportError(response.getFailure());
            return StoreResult.generalError(response.getFailureDetail());
        }

        return StoreResult.success(entity);
    }


    /**
     * Updates an entity on the control plane.
     *
     * @param entity the entity to update.
     * @param clazz the target entity type.
     * @return a store result with the updated entity, or a not-found or general error.
     */
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


    /**
     * Returns the message template used when an entity already exists.
     *
     * @return the message template used when an entity already exists.
     */
    protected abstract String getExistsTemplate();


    /**
     * Returns the message template used when an entity is not found.
     *
     * @return the message template used when an entity is not found.
     */
    protected abstract String getNotFoundTemplate();


    private ServiceResult<String> executeRequest(Request request) {
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

        }
        catch (IOException controlPlaneConnectionException) {
            return ServiceResult.unexpected(controlPlaneConnectionException.getMessage());
        }
    }


    private void reportError(ServiceFailure failure) {
        monitor.severe(String.format("%s: %s", failure.getReason(), failure.getFailureDetail()));
    }


    /**
     * Abstract builder for control-plane connection handlers.
     *
     * @param <T> the handler type produced by this builder.
     * @param <B> the builder subtype for fluent chaining.
     */
    public abstract static class Builder<T extends ControlPlaneConnectionHandler<?>, B extends Builder<T, B>> {

        /** Default constructor. */
        public Builder() {
        }

        /** Resource name appended to the control-plane URL. */
        protected String resourceName;
        private EdcHttpClient httpClient;
        private Monitor monitor;
        private String managementUri;
        private AuthenticationMethod authenticationMethod;
        private Codec codec;
        private Vault vault;


        /**
         * Returns this builder instance for fluent chaining.
         *
         * @return this builder instance for fluent chaining.
         */
        protected abstract B self();


        /**
         * Creates the handler instance from the prepared dependencies.
         *
         * @param monitor the monitor used for logging.
         * @param httpClient the HTTP client used to execute requests.
         * @param codec the codec used to serialize and deserialize entities.
         * @param connection the control-plane connection used to build requests.
         * @return the created handler instance.
         */
        protected abstract T create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection);


        /**
         * Sets the monitor.
         *
         * @param v the monitor used for logging.
         * @return this builder.
         */
        public B monitor(Monitor v) {
            this.monitor = v;
            return self();
        }


        /**
         * Sets the codec.
         *
         * @param codec the codec used to serialize and deserialize entities.
         * @return this builder.
         */
        public B codec(Codec codec) {
            this.codec = codec;
            return self();
        }


        /**
         * Sets the HTTP client.
         *
         * @param v the HTTP client used to execute requests.
         * @return this builder.
         */
        public B httpClient(EdcHttpClient v) {
            this.httpClient = v;
            return self();
        }


        /**
         * Sets the management API URL of the control plane.
         *
         * @param managementUri the management API URL.
         * @return this builder.
         */
        public B managementUri(String managementUri) {
            this.managementUri = managementUri;
            return self();
        }


        /**
         * Sets the authentication method.
         *
         * @param authenticationMethod the authentication method applied to requests.
         * @return this builder.
         */
        public B authenticationMethod(AuthenticationMethod authenticationMethod) {
            this.authenticationMethod = authenticationMethod;
            return self();
        }


        /**
         * Sets the vault used to resolve authentication secrets.
         *
         * @param vault the vault.
         * @return this builder.
         */
        public B vault(Vault vault) {
            this.vault = vault;
            return self();
        }


        /**
         * Builds the handler instance from the configured dependencies.
         *
         * @return the constructed handler.
         */
        public T build() {
            Objects.requireNonNull(httpClient);
            Objects.requireNonNull(monitor);
            Objects.requireNonNull(codec);
            Objects.requireNonNull(managementUri);
            Objects.requireNonNull(authenticationMethod);

            ControlPlaneConnection connection = new ControlPlaneConnection(URI.create(managementUri), resourceName, vault, authenticationMethod);

            return create(monitor, httpClient, codec, connection);
        }

    }
}
