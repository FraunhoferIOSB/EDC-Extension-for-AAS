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
package de.fraunhofer.iosb.client.policy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.controlplane.services.spi.catalog.CatalogService;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;

import java.net.URL;
import java.util.Objects;

import static de.fraunhofer.iosb.client.ClientEndpoint.MISSING_QUERY_PARAMETER_MESSAGE;
import static de.fraunhofer.iosb.client.ClientEndpoint.MISSING_REQUEST_BODY_MESSAGE;
import static de.fraunhofer.iosb.client.policy.PolicyController.ACCEPTED_POLICIES_PATH;

/**
 * Provides API for accepted policy management and provider dataset retrieval.
 * For documentation see {@link de.fraunhofer.iosb.client.ClientEndpoint}
 */
@Consumes({MediaType.APPLICATION_JSON})
@Path(ACCEPTED_POLICIES_PATH)
public class PolicyController {

    static final String ACCEPTED_POLICIES_PATH = "acceptedPolicies";

    private final PolicyDefinitionStore policyDefinitionStore;
    private final PolicyService policyService;
    private final Monitor monitor;

    public PolicyController(Monitor monitor, CatalogService catalogService,
                            TypeTransformerRegistry typeTransformerRegistry, Config systemConfig) {
        var config = new PolicyServiceConfig(systemConfig);

        this.monitor = monitor.withPrefix("Client PolicyController");
        this.policyDefinitionStore = new PolicyDefinitionStore(monitor, config.getAcceptedPolicyDefinitionsPath());
        this.policyService = new PolicyService(catalogService, typeTransformerRegistry, config,
                this.policyDefinitionStore, monitor);
    }

    public Dataset getDataset(String counterPartyId, URL counterPartyUrl, String assetId) throws InterruptedException {
        return policyService.getDatasetForAssetId(counterPartyId, counterPartyUrl, assetId);
    }

    /**
     * Return policyDefinition for assetId that match any policyDefinitions' policy
     * of
     * the services' policyDefinitionStore instance containing user added
     * policyDefinitions.
     * If more than one policyDefinitions are provided by the provider
     * connector, an AmbiguousOrNullException will be thrown.
     *
     * @param counterPartyId  Provider of the asset. (id)
     * @param counterPartyUrl Provider of the asset. (url)
     * @param assetId         Asset ID of the asset whose contract should be fetched.
     * @return One policyDefinition offered by the provider for the given assetId.
     */
    public Result<ContractOffer> getAcceptableContractOfferForAssetId(String counterPartyId, URL counterPartyUrl, String assetId) {
        return policyService.getAcceptableContractOfferForAssetId(counterPartyId, counterPartyUrl, assetId);
    }

    /**
     * Adds an accepted contractOffer to match when checking a provider
     * contractOffer. Only the policies' rules are relevant.
     *
     * @param policyDefinitions accepted policyDefinitions
     * @return "OK"-response if requestBody is not empty
     */
    @POST
    public Response addAcceptedPolicyDefinitions(PolicyDefinition[] policyDefinitions) {
        monitor.info("POST /%s".formatted(ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinitions)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_REQUEST_BODY_MESSAGE.formatted("PolicyDefinition[]")).build();
        }

        policyDefinitionStore.putPolicyDefinitions(policyDefinitions);
        return Response.ok().build();
    }

    /**
     * Returns accepted policyDefinitions as a list
     *
     * @return Accepted policyDefinitions list
     */
    @GET
    public Response getAcceptedPolicyDefinitions() {
        monitor.info("GET /%s".formatted(ACCEPTED_POLICIES_PATH));
        return Response.ok(policyDefinitionStore.getPolicyDefinitions()).build();
    }

    /**
     * Removes an accepted policyDefinitions.
     *
     * @param policyDefinitionId ID of policyDefinition to be removed
     * @return PolicyDefinitionId of removed policyDefinition or 404
     */
    @DELETE
    public Response deleteAcceptedPolicyDefinition(String policyDefinitionId) {
        monitor.info("DELETE /%s".formatted(ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinitionId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("policyDefinitionId")).build();
        }

        if (policyDefinitionStore.removePolicyDefinition(policyDefinitionId).isPresent()) {
            return Response.ok(policyDefinitionId).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

    /**
     * Updates an accepted policyDefinition.
     * The policyDefinitionId must match with a stored policyDefinition.
     *
     * @param policyDefinition Updated policyDefinition
     * @return PolicyDefinitionId of updated policyDefinition or 404
     */
    @PUT
    public Response updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        monitor.info("PUT /%s".formatted(ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinition)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(MISSING_REQUEST_BODY_MESSAGE.formatted("PolicyDefinition")).build();
        }

        if (policyDefinitionStore.updatePolicyDefinitions(policyDefinition).isPresent()) {
            return Response.ok(policyDefinition.getId()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

}
