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
package de.fraunhofer.iosb.client;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;

import de.fraunhofer.iosb.client.dataTransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
import de.fraunhofer.iosb.client.util.Pair;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Automated contract negotiation
 */
@Consumes({ MediaType.APPLICATION_JSON, MediaType.WILDCARD })
@Produces({ MediaType.APPLICATION_JSON })
@Path(ClientEndpoint.AUTOMATED_PATH)
public class ClientEndpoint {
    /*
     * Root path for the client
     */
    public static final String AUTOMATED_PATH = "automated";
    private static final String ACCEPTED_POLICIES_PATH = "acceptedPolicies";
    private static final String DATASET_PATH = "dataset";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";
    private static final String TRANSFER_PATH = "transfer";

    private final Monitor monitor;

    private final NegotiationController negotiationController;
    private final PolicyController policyController;
    private final DataTransferController transferController;

    /**
     * Initialize a client endpoint.
     *
     * @param policyService     Finds out policy for a given asset id and provider
     *                          EDC url.
     * @param negotiator        Send contract offer, negotiation status watch.
     * @param transferInitiator Initiate transfer requests.
     */
    public ClientEndpoint(Monitor monitor,
            NegotiationController negotiationController,
            PolicyController policyController,
            DataTransferController transferController) {
        this.monitor = monitor;

        this.policyController = policyController;
        this.negotiationController = negotiationController;
        this.transferController = transferController;
    }

    /**
     * Return policyDefinition for assetId that match any policyDefinitions' policy
     * of the services' policyDefinitionStore instance containing user added
     * policyDefinitions. If more than one policyDefinitions are provided by the
     * provider connector, an AmbiguousOrNullException will be thrown.
     * 
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return One policyDefinition offered by the provider for the given assetId.
     * @throws InterruptedException Thread for agreementId was waiting, sleeping, or
     *                              otherwise occupied, and was
     *                              interrupted.
     */
    @GET
    @Path(DATASET_PATH)
    public Response getDataset(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("assetId") String assetId) {
        monitor.debug(format("[Client] Received a %s GET request", DATASET_PATH));

        if (Objects.isNull(providerUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Provider URL must not be null").build();
        }

        try {
            var dataset = policyController.getDataset(providerUrl, assetId);
            return Response.ok(dataset).build();
        } catch (InterruptedException interruptedException) {
            monitor.severe(format("[Client] Getting dataset failed for provider %s and asset %s", providerUrl,
                    assetId), interruptedException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(interruptedException.getMessage())
                    .build();
        }
    }

    /**
     * Negotiate a contract agreement using the given contract offer if no agreement
     * exists for this constellation.
     *
     * @param providerUrl Provider EDCs URL (DSP endpoint)
     * @param assetId     ID of the asset to be retrieved
     * @return Asset data
     */
    @POST
    @Path(NEGOTIATE_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("providerId") String providerId,
            @QueryParam("assetId") String assetId,
            @QueryParam("dataDestinationUrl") URL dataDestinationUrl) {
        monitor.debug(format("[Client] Received a %s POST request", NEGOTIATE_PATH));
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(assetId, "Asset ID must not be null");

        Pair<String, Policy> idPolicyPair; // id means contractOfferId
        try {
            idPolicyPair = policyController.getAcceptablePolicyForAssetId(providerUrl, assetId);
        } catch (InterruptedException negotiationException) {
            monitor.severe(format("[Client] Getting policies failed for provider %s and asset %s", providerUrl,
                    assetId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        var offer = ContractOffer.Builder.newInstance()
                .id(idPolicyPair.getFirst())
                .policy(idPolicyPair.getSecond())
                .assetId(assetId)
                .build();

        var contractRequest = ContractRequest.Builder.newInstance()
                .contractOffer(offer)
                .counterPartyAddress(providerUrl.toString())
                .providerId(providerId)
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .build();
        ContractAgreement agreement;

        try {
            agreement = negotiationController.negotiateContract(contractRequest);
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(format("[Client] Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    offer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        return getData(providerUrl, agreement.getId(), assetId, dataDestinationUrl);
    }

    /**
     * Negotiate a contract agreement using the given contract offer if no agreement
     * exists for this constellation.
     *
     * @param contractRequest The contract request to be sent.
     * @return contractAgreement of the completed negotiation.
     */
    @POST
    @Path(NEGOTIATE_CONTRACT_PATH)
    public Response negotiateContract(ContractRequest contractRequest) {
        monitor.debug(format("[Client] Received a %s POST request", NEGOTIATE_CONTRACT_PATH));
        Objects.requireNonNull(contractRequest, "ContractRequest must not be null");
        try {
            var agreement = negotiationController.negotiateContract(contractRequest);
            return Response.ok(agreement).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(
                    format("[Client] Negotiation failed for provider %s and contractRequest %s",
                            contractRequest.getProviderId(),
                            contractRequest.getContractOffer().getId()),
                    negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

    /**
     * Submits a data transfer request to the providerUrl.
     *
     * @param providerUrl The data provider's url
     * @param agreementId The basis of the data transfer.
     * @param assetId     The asset of which the data should be transferred
     * @return On success, the data of the desired asset. Else, returns an error
     *         message.
     */
    @GET
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("agreementId") String agreementId, @QueryParam("assetId") String assetId,
            @QueryParam("dataDestinationUrl") URL dataDestinationUrl) {
        monitor.debug(format("[Client] Received a %s GET request", TRANSFER_PATH));
        Objects.requireNonNull(providerUrl, "providerUrl must not be null");
        Objects.requireNonNull(agreementId, "agreementId must not be null");
        Objects.requireNonNull(assetId, "assetId must not be null");

        try {
            var data = transferController.initiateTransferProcess(providerUrl, agreementId, assetId, dataDestinationUrl);
            if (Objects.isNull(dataDestinationUrl)) {
                return Response.ok(data).build();
            } else {
                return Response.ok(format("Data transfer request to URL %s sent.", dataDestinationUrl)).build();
            }
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(format("[Client] Data transfer failed for provider %s and agreementId %s", providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

    /**
     * Adds an accepted contractOffer to match when checking a provider
     * contractOffer. Only the policies' rules are relevant.
     *
     * @param policyDefinitions policies' rules that are acceptable for an automated
     *                          contract negotiation
     */
    @POST
    @Path(ACCEPTED_POLICIES_PATH)
    public Response addAcceptedPolicyDefinitions(PolicyDefinition[] policyDefinitions) {
        monitor.debug(format("[Client] Received a %s POST request", ACCEPTED_POLICIES_PATH));

        if (Objects.isNull(policyDefinitions)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinitions array").build();
        }
        monitor.info(format("[Client] Adding %s accepted contract offers", policyDefinitions.length));

        policyController.addAcceptedPolicyDefinitions(policyDefinitions);
        return Response.ok().build();
    }

    /**
     * Return accepted policyDefinitions
     *
     * @return Accepted policyDefinitions list
     */
    @GET
    @Path(ACCEPTED_POLICIES_PATH)
    public Response getAcceptedPolicyDefinitions() {
        monitor.debug(format("[Client] Received a %s GET request", ACCEPTED_POLICIES_PATH));
        return Response.ok(policyController.getAcceptedPolicyDefinitions()).build();
    }

    /**
     * Removes an accepted policyDefinitions.
     *
     * @param policyDefinitions policyDefinition id of policyDefinition to be
     *                          removed
     * @return Optional containing removed policy definition or null
     */
    @DELETE
    @Path(ACCEPTED_POLICIES_PATH)
    public Response deleteAcceptedPolicyDefinition(@QueryParam("policyDefinitionId") String policyDefinitionId) {
        monitor.debug(
                format("[Client] Received a %s DELETE request for %s", ACCEPTED_POLICIES_PATH, policyDefinitionId));
        if (Objects.isNull(policyDefinitionId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinitionId parameter").build();
        }
        var removed = policyController.deleteAcceptedPolicyDefinition(policyDefinitionId);

        if (removed.isPresent()) {
            return Response.ok(policyDefinitionId).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

    /**
     * Updates an accepted policyDefinition.
     *
     * @param policyDefinitionId PolicyDefinition id of policyDefinition to be
     *                           updated
     * @param policyDefinition   Updated PolicyDefinition
     */
    @PUT
    @Path(ACCEPTED_POLICIES_PATH)
    public Response updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        monitor.debug(format("[Client] Received a %s PUT request", ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinition)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinition").build();
        }

        var updated = policyController.updateAcceptedPolicyDefinition(policyDefinition);
        if (updated.isPresent()) {
            return Response.ok(policyDefinition.getId()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }
}
