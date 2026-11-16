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
package de.fraunhofer.iosb.ilt.dataspace.client.policy;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;
import org.eclipse.edc.web.spi.exception.ObjectNotFoundException;

import java.net.URI;
import java.util.Collection;
import java.util.Objects;

import static de.fraunhofer.iosb.ilt.dataspace.client.DspClientController.MISSING_QUERY_PARAMETER_MESSAGE;
import static de.fraunhofer.iosb.ilt.dataspace.client.DspClientController.MISSING_REQUEST_BODY_MESSAGE;
import static de.fraunhofer.iosb.ilt.dataspace.client.constants.ApiConstants.ACCEPTED_POLICIES;
import static de.fraunhofer.iosb.ilt.dataspace.client.constants.ApiConstants.DSP_CLIENT_V1;
import static de.fraunhofer.iosb.ilt.dataspace.client.constants.ApiConstants.SEP;


/**
 * Provides API for accepted policy management.
 */
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
@Path(DSP_CLIENT_V1 + SEP + ACCEPTED_POLICIES)
public class PolicyController {

    private final AcceptedPolicyStore acceptedPolicyStore;

    /**
     * Creates a new PolicyController.
     *
     * @param acceptedPolicyStore store accepted policies.
     */
    public PolicyController(AcceptedPolicyStore acceptedPolicyStore) {
        this.acceptedPolicyStore = acceptedPolicyStore;
    }


    /**
     * Returns accepted policyDefinitions as a list
     *
     * @return Accepted policyDefinitions list
     */
    @GET
    public Collection<PolicyDefinition> getAcceptedPolicyDefinitions() {
        return acceptedPolicyStore.getPolicyDefinitions();
    }


    /**
     * Adds an accepted policy definition to match when checking a provider offer.
     *
     * @param policyDefinition accepted policy definition
     */
    @POST
    public void addAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        if (Objects.isNull(policyDefinition)) {
            throw new InvalidRequestException(MISSING_REQUEST_BODY_MESSAGE.formatted("policyDefinition"));
        }

        acceptedPolicyStore.addPolicyDefinition(policyDefinition);
    }


    /**
     * Removes an accepted policyDefinitions.
     *
     * @param policyDefinitionId ID of policyDefinition to be removed
     */
    @DELETE
    @Path("{policyDefinitionId}")
    public void deleteAcceptedPolicyDefinition(@PathParam("policyDefinitionId") String policyDefinitionId) {
        if (Objects.isNull(policyDefinitionId)) {
            throw new InvalidRequestException(String.format(MISSING_QUERY_PARAMETER_MESSAGE, "policyDefinitionId"));
        }
        if (!acceptedPolicyStore.removePolicyDefinition(policyDefinitionId)) {
            throw new ObjectNotFoundException(PolicyDefinition.class, policyDefinitionId);
        }
    }


    /**
     * Updates an accepted policyDefinition. The policyDefinitionId must match with a stored policyDefinition.
     *
     * @param policyDefinition Updated policyDefinition
     */
    @PUT
    public Response updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        if (Objects.isNull(policyDefinition)) {
            throw new InvalidRequestException(String.format(MISSING_REQUEST_BODY_MESSAGE, "PolicyDefinition"));
        }
        if (acceptedPolicyStore.updatePolicyDefinition(policyDefinition)) {
            return Response.created(URI.create(policyDefinition.getId())).build();
        }
        return Response.accepted(URI.create(policyDefinition.getId())).build();
    }
}
