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
package de.fraunhofer.iosb.edc.remote.stores.policy;

import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.Objects;
import java.util.stream.Stream;

public class RemotePolicyDefinitionStore extends ControlPlaneConnectionHandler implements PolicyDefinitionStore {

    private RemotePolicyDefinitionStore(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        super(monitor, httpClient, codec, connection);
    }


    @Override
    public PolicyDefinition findById(String policyId) {
        // Send request
        var request = controlPlane.prepareRequest(HttpMethod.GET, policyId, null);
        // Deserialize response
        var response = executeRequest(request);

        if (response.failed()) {
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.NOT_FOUND) {
                monitor.info(String.format(POLICY_NOT_FOUND, policyId));
            } else {
                monitor.warning(String.format("RemotePolicyDefinitionStore.findById failed: %s", response.getFailureDetail()));
            }

            return null;
        }

        var policyDefinitionJsonString = response.getContent();

        return codec.deserializePolicyDefinition(policyDefinitionJsonString);
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec spec) {
        String querySpecString = codec.serializeQuerySpec(spec);

        var request = controlPlane.prepareRequest(HttpMethod.POST, "request", querySpecString);

        var response = executeRequest(request);

        if (!response.succeeded()) {
            monitor.warning(String.format("Failed querying policy definitions: %s", response.getFailureDetail()));
            return Stream.empty();
        }

        var policyDefinitionsJsonString = response.getContent();

        return codec.deserializePolicyDefinitions(policyDefinitionsJsonString).stream();
    }

    @Override
    public StoreResult<PolicyDefinition> create(PolicyDefinition policyDefinition) {
        var policyDefinitionString = codec.serialize(policyDefinition);

        var request = controlPlane.prepareRequest(HttpMethod.POST, policyDefinitionString);

        var response = executeRequest(request);

        if (response.failed()) {
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.CONFLICT) {
                return StoreResult.alreadyExists(policyDefinition.getId());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailure().getReason(), response.getFailureDetail()));
        }

        // To be sure, we fetch the created policy definition from the control plane
        return StoreResult.success(findById(policyDefinition.getId()));
    }

    @Override
    public StoreResult<PolicyDefinition> update(PolicyDefinition policyDefinition) {
        var policyDefinitionString = codec.serialize(policyDefinition);

        var request = controlPlane.prepareRequest(HttpMethod.PUT, policyDefinition.getId(), policyDefinitionString);

        var response = executeRequest(request);

        if (response.failed()) {
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.NOT_FOUND) {
                return StoreResult.notFound(policyDefinition.getId());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailure().getReason(), response.getFailureDetail()));
        }

        return StoreResult.success(findById(policyDefinition.getId()));
    }

    @Override
    public StoreResult<PolicyDefinition> delete(String policyDefinitionId) {
        var toBeDeleted = findById(policyDefinitionId);

        if (toBeDeleted == null) {
            return StoreResult.notFound(policyDefinitionId);
        }

        var request = controlPlane.prepareRequest(HttpMethod.DELETE, policyDefinitionId, null);

        var response = executeRequest(request);

        if (response.failed()) {
            if (Objects.requireNonNull(response.getFailure().getReason()) == ServiceFailure.Reason.NOT_FOUND) {
                return StoreResult.notFound(policyDefinitionId);
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailure().getReason(), response.getFailureDetail()));
        }

        return StoreResult.success(toBeDeleted);
    }

    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemotePolicyDefinitionStore, Builder> {

        protected final String resourceName = "policydefinitions";

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected RemotePolicyDefinitionStore create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
            return new RemotePolicyDefinitionStore(monitor, httpClient, codec, connection);
        }
    }
}
