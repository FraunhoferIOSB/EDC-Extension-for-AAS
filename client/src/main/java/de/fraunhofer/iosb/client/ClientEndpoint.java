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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.client.datatransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
import de.fraunhofer.iosb.client.util.Pair;
import jakarta.json.Json;
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
import org.eclipse.edc.connector.controlplane.catalog.spi.Dataset;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.io.StringReader;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.http.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

/**
 * Automated contract negotiation
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.WILDCARD})
@Produces({MediaType.APPLICATION_JSON})
@Path(ClientEndpoint.AUTOMATED_PATH)
public class ClientEndpoint {
    /*
     * Root path for the client
     */
    public static final String AUTOMATED_PATH = "automated";
    private static final String ACCEPTED_POLICIES_PATH = "acceptedPolicies";
    private static final String OFFER_PATH = "offer";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";
    private static final String TRANSFER_PATH = "transfer";

    private final Monitor monitor;

    private final NegotiationController negotiationController;
    private final PolicyController policyController;
    private final DataTransferController transferController;

    private final ObjectMapper objectMapper;

    /**
     * Initialize a client endpoint.
     *
     * @param monitor               Logging functionality
     * @param policyController      Finds out policy for a given asset id and provider EDC url.
     * @param negotiationController Send contract offer, negotiation status watch.
     * @param transferController    Initiate transfer requests.
     */
    public ClientEndpoint(Monitor monitor,
                          NegotiationController negotiationController,
                          PolicyController policyController,
                          DataTransferController transferController) {
        this.monitor = monitor;

        this.policyController = policyController;
        this.negotiationController = negotiationController;
        this.transferController = transferController;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Return dataset for assetId that match any policyDefinition's policy
     * of the services' policyDefinitionStore instance containing user added
     * policyDefinitions. If more than one policyDefinitions are provided by the
     * provider connector, an AmbiguousOrNullException will be thrown.
     *
     * @param providerUrl Provider of the asset.
     * @param assetId     Asset ID of the asset whose contract should be fetched.
     * @return A dataset offered by the provider for the given assetId.
     */
    @GET
    @Path(OFFER_PATH)
    public Response getOffer(@QueryParam("providerUrl") URL providerUrl, @QueryParam("assetId") String assetId, @QueryParam("providerId") String counterPartyId) {
        Objects.requireNonNull(assetId, "Asset ID must not be null");


        monitor.debug(format("[Client] Received an %s GET request", OFFER_PATH));

        if (Objects.isNull(providerUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Provider URL must not be null").build();
        }

        try {
            var dataset = policyController.getDataset(counterPartyId, providerUrl, assetId);

            var parsedResponse = buildResponseFrom(dataset);
            return Response.ok(parsedResponse).build();

        } catch (InterruptedException interruptedException) {
            monitor.severe(format("[Client] Getting offer failed for provider %s and asset %s", providerUrl,
                    assetId), interruptedException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(interruptedException.getMessage())
                    .build();

        } catch (JsonProcessingException policyWriteException) {
            monitor.severe(format("[Client] Parsing policy failed for provider %s and asset %s", providerUrl,
                    assetId), policyWriteException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(policyWriteException.getMessage())
                    .build();

        }
    }

    private String buildResponseFrom(Dataset dataset) throws JsonProcessingException {
        var offer = dataset.getOffers().entrySet().stream().findFirst().orElseThrow();

        // Build negotiation request body for the user
        var policyString = objectMapper.writeValueAsString(offer.getValue());
        var policyJson = Json.createReader(new StringReader(policyString)).read();

        return Json.createObjectBuilder()
                .add("id", offer.getKey())
                .add("policy", policyJson)
                .add("assetId", offer.getValue().getTarget())
                .build()
                .toString();
    }

    /**
     * Negotiate a contract agreement using the given contract offer if no agreement
     * exists for this constellation.
     *
     * @param counterPartyUrl Provider EDCs URL (DSP endpoint)
     * @param counterPartyId  Provider EDCs ID
     * @param assetId         ID of the asset to be retrieved
     * @param dataAddress     DataAddress of destination data sink.
     * @return Asset data
     */
    @POST
    @Path(NEGOTIATE_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL counterPartyUrl,
                                      @QueryParam("providerId") String counterPartyId,
                                      @QueryParam("assetId") String assetId,
                                      DataAddress dataAddress) {
        monitor.debug(format("[Client] Received a %s POST request", NEGOTIATE_PATH));
        Objects.requireNonNull(counterPartyUrl, "Provider URL must not be null");
        Objects.requireNonNull(counterPartyId, "Provider ID must not be null");
        Objects.requireNonNull(assetId, "Asset ID must not be null");

        Pair<String, Policy> idPolicyPair; // id means contractOfferId
        try {
            idPolicyPair = policyController.getAcceptablePolicyForAssetId(counterPartyId, counterPartyUrl, assetId);
        } catch (InterruptedException negotiationException) {
            monitor.severe(format("[Client] Getting policies failed for provider %s and asset %s", counterPartyUrl,
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
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .counterPartyAddress(counterPartyUrl.toString())
                .contractOffer(offer)
                .build();
        ContractAgreement agreement;

        try {
            agreement = negotiationController.negotiateContract(contractRequest);
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(format("[Client] Negotiation failed for provider %s and contractOffer %s", counterPartyUrl,
                    offer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        return getData(counterPartyUrl, agreement.getId(), dataAddress);
    }

    /**
     * Negotiates a contract agreement using the given contract offer if no agreement
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
            // Sanitize response (only ID is relevant here)
            var agreementResponse = Json.createObjectBuilder()
                    .add("agreement-id", agreement.getId())
                    .build()
                    .toString();
            return Response.ok(agreementResponse).build();
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
     * In the future this could be replaced with the
     * <a href="https://www.ietf.org/archive/id/draft-ietf-httpbis-safe-method-w-body-02.html">HTTP QUERY method</a>
     *
     * @param providerUrl The data provider's url
     * @param agreementId The basis of the data transfer.
     * @param dataAddress URL of destination data sink.
     * @return On success, the data of the desired asset. Else, returns an error message.
     */
    @POST
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
                            @QueryParam("agreementId") String agreementId,
                            DataAddress dataAddress) {
        monitor.debug(format("[Client] Received a %s GET request", TRANSFER_PATH));
        Objects.requireNonNull(providerUrl, "providerUrl must not be null");
        Objects.requireNonNull(agreementId, "agreementId must not be null");

        try {
            var data = transferController.initiateTransferProcess(providerUrl, agreementId, dataAddress);
            if (Objects.isNull(dataAddress)) {
                return Response.ok(data).build();
            } else {
                return Response.ok("Data transfer request sent.").build();
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
     * @param policyDefinitions accepted policyDefinitions
     * @return "OK"-response if requestBody is not empty
     */
    @POST
    @Path(ACCEPTED_POLICIES_PATH)
    public Response addAcceptedPolicyDefinitions(PolicyDefinition[] policyDefinitions) {
        monitor.debug(format("[Client] Received a %s POST request", ACCEPTED_POLICIES_PATH));
        Objects.requireNonNull(policyDefinitions, "policyDefinitions (request body) must not be null");

        policyController.addAcceptedPolicyDefinitions(policyDefinitions);
        return Response.ok().build();
    }

    /**
     * Returns accepted policyDefinitions as list
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
     * @param policyDefinitionId ID of policyDefinition to be removed
     * @return PolicyDefinitionId of removed policyDefinition or 404
     */
    @DELETE
    @Path(ACCEPTED_POLICIES_PATH)
    public Response deleteAcceptedPolicyDefinition(@QueryParam("policyDefinitionId") String policyDefinitionId) {
        monitor.debug(
                format("[Client] Received a %s DELETE request for %s", ACCEPTED_POLICIES_PATH, policyDefinitionId));
        Objects.requireNonNull(policyDefinitionId, "policyDefinitionId must not be null");

        if (policyController.deleteAcceptedPolicyDefinition(policyDefinitionId).isPresent()) {
            // Found policyDefinition with same ID
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
    @Path(ACCEPTED_POLICIES_PATH)
    public Response updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        monitor.debug(format("[Client] Received a %s PUT request", ACCEPTED_POLICIES_PATH));
        Objects.requireNonNull(policyDefinition, "policyDefinition (request body) must not be null");

        if (policyController.updateAcceptedPolicyDefinition(policyDefinition).isPresent()) {
            // Found policyDefinition with same ID
            return Response.ok(policyDefinition.getId()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }
}
