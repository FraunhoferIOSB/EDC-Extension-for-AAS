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
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.contract.spi.offer.store.ContractDefinitionStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractDefinition;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;


public class RemoteContractDefinitionStore extends ControlPlaneConnectionHandler<ContractDefinition> implements ContractDefinitionStore {

    private static final String MGMT_API_RESOURCE_ACCESSOR = "contractdefinitions";


    public RemoteContractDefinitionStore(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        super(monitor, httpClient, codec, connection);
    }


    @Override
    public @NotNull Stream<ContractDefinition> findAll(QuerySpec querySpec) {
        return queryEntities(querySpec, ContractDefinition.class);
    }


    @Override
    public ContractDefinition findById(String contractDefinitionId) {
        return findById(contractDefinitionId, ContractDefinition.class);
    }


    /**
     * Extends ContractDefinitionStore return values by generalError
     *
     * @param contractDefinition The {@link ContractDefinition} to store
     * @return Same as ContractDefinitionStore, but generalError if connection to control-plane fails
     */
    @Override
    public StoreResult<Void> save(ContractDefinition contractDefinition) {
        var result = createEntity(contractDefinition);

        // This is the only case where Void is returned.
        if (result.succeeded()) {
            return StoreResult.success();
        }
        return StoreResult.alreadyExists(result.getFailureDetail());
    }


    @Override
    public StoreResult<Void> update(ContractDefinition contractDefinition) {
        var result = updateEntity(contractDefinition, ContractDefinition.class);

        // This is the only case where Void is returned.
        if (result.succeeded()) {
            return StoreResult.success();
        }
        return StoreResult.notFound(result.getFailureDetail());
    }


    @Override
    public StoreResult<ContractDefinition> deleteById(String contractDefinitionId) {
        return deleteById(contractDefinitionId, ContractDefinition.class);
    }


    @Override
    protected String getExistsTemplate() {
        return CONTRACT_DEFINITION_EXISTS;
    }


    @Override
    protected String getNotFoundTemplate() {
        return CONTRACT_DEFINITION_NOT_FOUND;
    }


    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemoteContractDefinitionStore, RemoteContractDefinitionStore.Builder> {

        @Override
        protected RemoteContractDefinitionStore.Builder self() {
            return this;
        }


        public RemoteContractDefinitionStore build() {
            this.resourceName = MGMT_API_RESOURCE_ACCESSOR;
            return super.build();
        }


        @Override
        protected RemoteContractDefinitionStore create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
            return new RemoteContractDefinitionStore(monitor, httpClient, codec, connection);
        }
    }
}
