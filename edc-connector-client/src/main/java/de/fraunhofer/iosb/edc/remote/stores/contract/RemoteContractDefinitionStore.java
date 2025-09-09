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
package de.fraunhofer.iosb.edc.remote.stores.contract;

import de.fraunhofer.iosb.edc.remote.ControlPlaneConnection;
import de.fraunhofer.iosb.edc.remote.HttpMethod;
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

public class RemoteContractDefinitionStore extends ControlPlaneConnectionHandler implements ContractDefinitionStore {


    public RemoteContractDefinitionStore(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        super(monitor, httpClient, codec, connection);
    }

    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec querySpec) {
        String querySpecString = codec.serializeQuerySpec(querySpec);

        var request = controlPlane.prepareRequest(HttpMethod.POST, "request", querySpecString);

        var response = executeRequest(request);

        if (response.failed()) {
            monitor.warning(String.format("Failed querying contract definitions. %s: %s", response.reason(), response.getFailureDetail()));
            return Stream.empty();
        }

        var contractDefinitionsJsonString = response.getContent();

        return codec.deserializeContractDefinitions(contractDefinitionsJsonString).stream();
    }

    @Override
    public ContractDefinition findById(String contractDefinitionId) {

        var request = controlPlane.prepareRequest(HttpMethod.GET, contractDefinitionId, null);

        var response = executeRequest(request);

        if (response.failed()) {
            monitor.debug(String.format(CONTRACT_DEFINITION_NOT_FOUND, contractDefinitionId).concat(String.format(". %s: %s", response.reason(),
                    response.getFailureDetail())));
            return null;
        }

        var contractDefinitionJsonString = response.getContent();

        return codec.deserializeContractDefinition(contractDefinitionJsonString);
    }

    /**
     * Extends ContractDefinitionStore return values by generalError
     *
     * @param contractDefinition The {@link ContractDefinition} to store
     * @return Same as ContractDefinitionStore, but generalError if connection to control-plane fails
     */
    @Override
    public StoreResult<Void> save(ContractDefinition contractDefinition) {
        var contractDefinitionJsonString = codec.serialize(contractDefinition);

        var request = controlPlane.prepareRequest(HttpMethod.POST, contractDefinitionJsonString);

        var response = executeRequest(request);

        if (response.failed()) {
            if (response.reason().equals(ServiceFailure.Reason.CONFLICT)) {
                return StoreResult.alreadyExists(contractDefinition.getId());
            }
            return StoreResult.generalError(String.format("Failed saving contract definition. %s: %s", response.reason(),
                    response.getFailureDetail()));
        }

        return StoreResult.success();
    }

    @Override
    public StoreResult<Void> update(ContractDefinition contractDefinition) {
        var contractDefinitionJsonString = codec.serialize(contractDefinition);

        var request = controlPlane.prepareRequest(HttpMethod.PUT, contractDefinitionJsonString);

        var response = executeRequest(request);

        if (response.failed()) {
            if (response.reason().equals(ServiceFailure.Reason.NOT_FOUND)) {
                return StoreResult.notFound(response.getFailureDetail());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailureDetail()));
        }

        return StoreResult.success();
    }

    @Override
    public StoreResult<ContractDefinition> deleteById(String contractDefinitionId) {
        var contractDefinition = findById(contractDefinitionId);

        var request = controlPlane.prepareRequest(HttpMethod.DELETE, contractDefinitionId, null);

        var response = executeRequest(request);

        if (response.failed()) {
            if (response.reason().equals(ServiceFailure.Reason.NOT_FOUND)) {
                return StoreResult.notFound(response.getFailureDetail());
            }
            throw new EdcException(String.format(UNEXPECTED_ERROR, response.getFailureDetail()));
        }

        return StoreResult.success(contractDefinition);
    }


    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemoteContractDefinitionStore, RemoteContractDefinitionStore.Builder> {


        @Override
        protected RemoteContractDefinitionStore.Builder self() {
            return this;
        }

        public RemoteContractDefinitionStore build() {
            this.resourceName = "contractdefinitions";
            return super.build();
        }

        @Override
        protected RemoteContractDefinitionStore create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
            return new RemoteContractDefinitionStore(monitor, httpClient, codec, connection);
        }
    }
}
