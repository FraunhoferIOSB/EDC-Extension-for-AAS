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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fraunhofer.iosb.app.edc.ContractHandler;
import de.fraunhofer.iosb.app.edc.ResourceHandler;
import de.fraunhofer.iosb.app.util.Pair;
import jakarta.ws.rs.core.Response;
import okhttp3.OkHttpClient;
import org.eclipse.dataspaceconnector.spi.asset.AssetLoader;
import org.eclipse.dataspaceconnector.spi.contract.offer.store.ContractDefinitionStore;
import org.eclipse.dataspaceconnector.spi.policy.store.PolicyDefinitionStore;

import java.util.Objects;

import static java.lang.String.format;

/**
 * Controls communication with EDC (EDC contracts and EDC assets)
 */
public class ResourceController {

    private final ResourceHandler resourceAgent;
    private final ContractHandler contractHandler;

    public ResourceController(AssetLoader assetLoader, OkHttpClient okHttpClient, ContractDefinitionStore contractStore,
            PolicyDefinitionStore policyStore) {
        resourceAgent = new ResourceHandler(assetLoader, okHttpClient);
        contractHandler = new ContractHandler(contractStore, policyStore);
    }

    /**
     * Registers resource at EDC with default contract, return contract id of new
     * asset

     * @param linkToResource Link to resource to be registered
     * @return contract id
     */
    public Pair<String, String> createResource(String linkToResource) {
        final var assetId = resourceAgent.createAsset(linkToResource);
        final var contractId = contractHandler.registerAssetToDefaultContract(assetId);
        return new Pair<>(assetId, contractId);
    }

    /**
     * Registers resource at EDC

     * @param linkToResource       Link to resource to be registered
     * @param contractDefinitionId Contract ID of contract to be associated to new
     *                             asset
     * @return asset id and linked contract id of new asset
     */
    public Pair<String, String> createResource(String linkToResource, String contractDefinitionId) {
        final var assetId = resourceAgent.createAsset(linkToResource);
        final var contractId = contractHandler.registerAssetToContract(assetId, contractDefinitionId);
        return new Pair<>(assetId, contractId);
    }

    /**
     * Disassociate asset from all contracts, then delete it from EDC AssetIndex.

     * @param assetId Asset ID of asset to be deleted
     */
    public void deleteAsset(String assetId) {
        contractHandler.unregisterAssetFromAllContracts(assetId);
        resourceAgent.deleteAsset(assetId);
    }

    /**
     * Add link to contract the asset shall be associated to.

     * @param contractId contract id
     * @param assetId    asset id
     */
    public Response registerAssetToContract(final String contractId, final String assetId) {
        if (Objects.nonNull(contractHandler.registerAssetToContract(contractId, assetId))) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND.getStatusCode(),
                format("Contract with contract ID %s not found", contractId)).build();
    }

    /**
     * Disassociate an asset from a contract by ID.

     * @param assetId    asset id
     * @param contractId contract id
     */
    public void unregisterAssetFromContract(final String assetId, final String contractId) {
        contractHandler.unregisterAssetFromContract(assetId, contractId);
    }

    /**
     * Unregister an asset from every contract, by ID.

     * @param assetId    asset id of the asset to be unregistered
     */
    public void unregisterAssetFromAllContracts(final String assetId) {
        contractHandler.unregisterAssetFromAllContracts(assetId);
    }

    /**
     * Create a contract containing the given policies and contractpolicyId

     * @param contractDefinitionId contractDefinitionId
     * @param accessPolicyId       Access policy of the new contract
     * @param contractPolicyId     Contract policy of the new contract
     * @return contract UID
     */
    public String createContract(String contractDefinitionId, String accessPolicyId, String contractPolicyId) {
        return contractHandler.addContractDefinition(contractDefinitionId, accessPolicyId, contractPolicyId);
    }

    /**
     * Create a contract given its serialized form

     * @param contractSerialized Serialized contract
     * @return the new contract's UID
     */
    public String createContract(String contractSerialized) throws JsonProcessingException {
        return contractHandler.addContractDefinition(contractSerialized);
    }

    /**
     * Removes the reference of an asset to this contract

     * @param contractId The contract id
     * @param assetId    The asset id
     */
    public void deleteContractReference(String contractId, String assetId) {
        contractHandler.deleteContractReference(contractId, assetId);
    }

    /**
     * Removes a contract

     * @param contractId The contract id
     */
    public void deleteContract(String contractId) {
        contractHandler.deleteContractDefinition(contractId);
    }

}
