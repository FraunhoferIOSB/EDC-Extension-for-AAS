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

import de.fraunhofer.iosb.client.datatransfer.DataTransferController;
import de.fraunhofer.iosb.client.negotiation.NegotiationController;
import de.fraunhofer.iosb.client.policy.PolicyController;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.DataAddress;

import java.net.URL;
import java.util.Map;
import java.util.Objects;

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
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";

    public static final String MISSING_QUERY_PARAMETER_MESSAGE = "Missing query parameter. Required parameters: %s";
    public static final String MISSING_REQUEST_BODY_MESSAGE = "Missing request body of type %s";

    private final Monitor monitor;

    private final NegotiationController negotiationController;
    private final PolicyController policyController;
    private final DataTransferController transferController;


    private ClientEndpoint(Monitor monitor,
                           NegotiationController negotiationController,
                           PolicyController policyController,
                           DataTransferController transferController) {
        this.monitor = monitor;
        this.policyController = policyController;
        this.negotiationController = negotiationController;
        this.transferController = transferController;
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
        monitor.info("POST /%s".formatted(NEGOTIATE_PATH));
        if (counterPartyUrl == null || counterPartyId == null || assetId == null ||
                assetId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_QUERY_PARAMETER_MESSAGE.formatted("providerUrl, counterPartyId, assetId")).build();
        }
        if (dataAddress != null && dataAddress.getProperties().get("operation") != null) {
            String operation;
            try {
                operation = nonNullNonEmptyObjectMapper.writeValueAsString(dataAddress.getProperties().get("operation"
                ));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            dataAddress = DataAddress.Builder.newInstance()
                    .type(dataAddress.getType())
                    .properties(dataAddress.getProperties())
                    .property("operation", operation)
                    .build();
        }

        Result<ContractOffer> contractOfferResult =
                policyController.getAcceptableContractOfferForAssetId(counterPartyId, counterPartyUrl, assetId);

        if (contractOfferResult.failed()) {
            monitor.severe("Getting policies failed for provider %s and asset %s: %s".formatted(
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
            monitor.severe("Negotiation failed for provider %s and contractOffer %s: %s".formatted(
                    counterPartyUrl, contractOfferResult.getContent().getId(), agreementResult.getFailureDetail()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(agreementResult.getFailureDetail()).build();
        }

        return transferController.getData(counterPartyUrl, agreementResult.getContent().getId(), dataAddress);
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
        monitor.info("POST /%s".formatted(NEGOTIATE_CONTRACT_PATH));
        if (Objects.isNull(contractRequest)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(MISSING_REQUEST_BODY_MESSAGE.formatted("ContractRequest")).build();
        }

        Result<ContractAgreement> agreementResult = negotiationController.negotiateContract(contractRequest);

        if (agreementResult.failed()) {
            monitor.severe("Negotiation failed for provider %s and contractOffer %s: %s".formatted(
                    contractRequest.getProviderId(), contractRequest.getContractOffer().getId(),
                    agreementResult.getFailureDetail()));
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(agreementResult.getFailureDetail()).build();
        }

        return Response.ok(Map.of("agreement-id", agreementResult.getContent().getId())).build();
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
