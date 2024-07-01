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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.client.datatransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
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
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

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

    private static final String MISSING_QUERY_PARAMETER_MESSAGE = "Missing query parameter. Required parameters: %s";
    private static final String MISSING_REQUEST_BODY_MESSAGE = "Missing request body of type %s";

    private final Monitor monitor;

    private final NegotiationController negotiationController;
    private final PolicyController policyController;
    private final DataTransferController transferController;

    private final ObjectMapper nonNullNonEmptyObjectMapper;

    private ClientEndpoint(Monitor monitor,
                           NegotiationController negotiationController,
                           PolicyController policyController,
                           DataTransferController transferController) {
        this.monitor = monitor;
        this.policyController = policyController;
        this.negotiationController = negotiationController;
        this.transferController = transferController;
        nonNullNonEmptyObjectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
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
        monitor.info("[Client] Received an %s GET request".formatted(OFFER_PATH));
        if (Objects.isNull(assetId) || Objects.isNull(providerUrl)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("assetId, providerId")).build();
        }

        Dataset dataset;
        try {
            dataset = policyController.getDataset(counterPartyId, providerUrl, assetId);
        } catch (InterruptedException interruptedException) {
            monitor.severe("[Client] Getting offer failed for provider %s and asset %s"
                    .formatted(providerUrl, assetId), interruptedException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(interruptedException.getMessage())
                    .build();
        }

        try {
            return Response.ok(nonNullNonEmptyObjectMapper.writeValueAsString(buildResponseFrom(dataset))).build();
        } catch (JsonProcessingException e) {
            return Response.serverError().entity("Could not serialize contractOffer").build();
        }

    }

    private ContractOffer buildResponseFrom(Dataset dataset) {
        return dataset.getOffers().entrySet().stream()
                .findFirst()
                .map(entry ->
                        ContractOffer.Builder.newInstance()
                                .id(entry.getKey())
                                .policy(entry.getValue())
                                .assetId(entry.getValue().getTarget())
                                .build())
                .orElseThrow(() -> new EdcException("Failed building response policyDefinition"));
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
        monitor.info("[Client] Received a %s POST request".formatted(NEGOTIATE_PATH));
        if (Objects.isNull(counterPartyUrl) || Objects.isNull(counterPartyId) || Objects.isNull(assetId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("providerUrl, counterPartyId, assetId")).build();
        }

        Result<ContractOffer> contractOfferResult = policyController.getAcceptableContractOfferForAssetId(counterPartyId, counterPartyUrl, assetId);

        if (contractOfferResult.failed()) {
            monitor.severe("[Client] Getting policies failed for provider %s and asset %s: %s".formatted(
                    counterPartyUrl, assetId, contractOfferResult.getFailureDetail()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(contractOfferResult.getFailureDetail())
                    .build();
        }

        var contractRequest = ContractRequest.Builder.newInstance()
                .protocol(DATASPACE_PROTOCOL_HTTP)
                .counterPartyAddress(counterPartyUrl.toString())
                .contractOffer(contractOfferResult.getContent())
                .build();

        Result<ContractAgreement> agreementResult = negotiationController.negotiateContract(contractRequest);

        if (agreementResult.failed()) {
            monitor.severe("[Client] Negotiation failed for provider %s and contractOffer %s: %s".formatted(
                    counterPartyUrl, contractOfferResult.getContent().getId(), agreementResult.getFailureDetail()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(agreementResult.getFailureDetail()).build();
        }

        return getData(counterPartyUrl, agreementResult.getContent().getId(), dataAddress);
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
        monitor.info("[Client] Received a %s POST request".formatted(NEGOTIATE_CONTRACT_PATH));
        if (Objects.isNull(contractRequest)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_REQUEST_BODY_MESSAGE.formatted("ContractRequest")).build();
        }

        Result<ContractAgreement> agreementResult = negotiationController.negotiateContract(contractRequest);

        if (agreementResult.failed()) {
            monitor.severe("[Client] Negotiation failed for provider %s and contractOffer %s: %s".formatted(
                    contractRequest.getProviderId(), contractRequest.getContractOffer().getId(), agreementResult.getFailureDetail()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(agreementResult.getFailureDetail()).build();
        }

        return Response.ok(Map.of("agreement-id", agreementResult.getContent().getId())).build();
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
        monitor.info("[Client] Received a %s GET request".formatted(TRANSFER_PATH));
        if (Objects.isNull(providerUrl) || Objects.isNull(agreementId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("providerUrl, agreementId")).build();
        }

        try {
            var data = transferController.initiateTransferProcess(providerUrl, agreementId, dataAddress);
            if (Objects.isNull(dataAddress)) {
                return Response.ok(data).build();
            } else {
                return Response.ok("Data transfer request sent.").build();
            }
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe("[Client] Data transfer failed for provider %s and agreementId %s".formatted(providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(negotiationException.getMessage())
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
        monitor.info("[Client] Received a %s POST request".formatted(ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinitions)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_REQUEST_BODY_MESSAGE.formatted("PolicyDefinition[]")).build();
        }

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
        monitor.info("[Client] Received a %s GET request".formatted(ACCEPTED_POLICIES_PATH));
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
        monitor.info("[Client] Received a %s DELETE request for %s".formatted(ACCEPTED_POLICIES_PATH, policyDefinitionId));
        if (Objects.isNull(policyDefinitionId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("policyDefinitionId")).build();
        }

        if (policyController.deleteAcceptedPolicyDefinition(policyDefinitionId).isPresent()) {
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
        monitor.info("[Client] Received a %s PUT request".formatted(ACCEPTED_POLICIES_PATH));
        if (Objects.isNull(policyDefinition)) {
            return Response.status(Response.Status.BAD_REQUEST).entity(MISSING_REQUEST_BODY_MESSAGE.formatted("PolicyDefinition")).build();
        }

        if (policyController.updateAcceptedPolicyDefinition(policyDefinition).isPresent()) {
            return Response.ok(policyDefinition.getId()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

    public static class Builder {
        private Monitor monitor;
        private NegotiationController negotiationController;
        private PolicyController policyController;
        private DataTransferController transferController;

        private Builder() {
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder monitor(Monitor monitor) {
            this.monitor = monitor;
            return this;
        }

        public Builder negotiationController(NegotiationController negotiationController) {
            this.negotiationController = negotiationController;
            return this;
        }

        public Builder policyController(PolicyController policyController) {
            this.policyController = policyController;
            return this;
        }

        public Builder transferController(DataTransferController transferController) {
            this.transferController = transferController;
            return this;
        }

        public ClientEndpoint build() {
            return new ClientEndpoint(monitor, negotiationController, policyController, transferController);
        }

    }
}
