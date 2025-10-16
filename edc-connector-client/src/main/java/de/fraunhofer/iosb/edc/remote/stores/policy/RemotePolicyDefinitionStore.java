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
import de.fraunhofer.iosb.edc.remote.stores.ControlPlaneConnectionHandler;
import de.fraunhofer.iosb.edc.remote.transform.Codec;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.policy.spi.store.PolicyDefinitionStore;
import org.eclipse.edc.http.spi.EdcHttpClient;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.StoreResult;

import java.util.stream.Stream;

public class RemotePolicyDefinitionStore extends ControlPlaneConnectionHandler<PolicyDefinition> implements PolicyDefinitionStore {

    private static final String MGMT_API_RESOURCE_ACCESSOR = "policydefinitions";

    private RemotePolicyDefinitionStore(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
        super(monitor, httpClient, codec, connection);
    }


    @Override
    public PolicyDefinition findById(String policyId) {
        return findById(policyId, PolicyDefinition.class);
    }

    @Override
    public Stream<PolicyDefinition> findAll(QuerySpec spec) {
        return queryEntities(spec, PolicyDefinition.class);
    }

    @Override
    public StoreResult<PolicyDefinition> create(PolicyDefinition policyDefinition) {
        StoreResult<Void> createResult = createEntity(policyDefinition);
        if (createResult.succeeded()) {
            return StoreResult.success(policyDefinition);
        }

        // Since you cannot create StoreResult<T> from StoreResult<U>:
        String failureDetail = createResult.getFailureDetail();
        return switch (createResult.reason()) {
            case NOT_FOUND -> StoreResult.notFound(createResult.getFailureDetail());
            case ALREADY_EXISTS -> StoreResult.alreadyExists(createResult.getFailureDetail());
            case DUPLICATE_KEYS -> StoreResult.duplicateKeys(failureDetail);
            case ALREADY_LEASED -> StoreResult.alreadyLeased(failureDetail);
            default -> StoreResult.generalError(failureDetail);
        };
    }

    @Override
    public StoreResult<PolicyDefinition> update(PolicyDefinition policyDefinition) {
        return updateEntity(policyDefinition, PolicyDefinition.class);
    }

    @Override
    public StoreResult<PolicyDefinition> delete(String policyDefinitionId) {
        return deleteById(policyDefinitionId, PolicyDefinition.class);
    }


    @Override
    protected String getExistsTemplate() {
        return POLICY_ALREADY_EXISTS;
    }


    @Override
    protected String getNotFoundTemplate() {
        return POLICY_NOT_FOUND;
    }


    public static class Builder extends ControlPlaneConnectionHandler.Builder<RemotePolicyDefinitionStore, Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public RemotePolicyDefinitionStore build() {
            this.resourceName = MGMT_API_RESOURCE_ACCESSOR;
            return super.build();
        }

        @Override
        protected RemotePolicyDefinitionStore create(Monitor monitor, EdcHttpClient httpClient, Codec codec, ControlPlaneConnection connection) {
            return new RemotePolicyDefinitionStore(monitor, httpClient, codec, connection);
        }
    }
}
