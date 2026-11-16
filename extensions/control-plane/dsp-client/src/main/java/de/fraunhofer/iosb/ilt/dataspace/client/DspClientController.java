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
package de.fraunhofer.iosb.ilt.dataspace.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fraunhofer.iosb.ilt.dataspace.client.constants.ApiConstants;
import de.fraunhofer.iosb.ilt.dataspace.client.dto.PullRequestDTO;
import de.fraunhofer.iosb.ilt.dataspace.client.negotiation.Negotiator;
import de.fraunhofer.iosb.ilt.dataspace.client.policy.PolicyService;
import de.fraunhofer.iosb.ilt.dataspace.client.transfer.pull.PullTransferInitiator;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.controlplane.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.ServiceResult;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.exception.InvalidRequestException;

import static java.util.Optional.ofNullable;
import static org.eclipse.edc.protocol.dsp.spi.type.Dsp2025Constants.DATASPACE_PROTOCOL_HTTP_V_2025_1;


/**
 * Automated contract negotiation
 */
@Path("v1/" + ApiConstants.DSP_CLIENT_V1)
public class DspClientController {
    /** Message template for missing query parameters. */
    public static final String MISSING_QUERY_PARAMETER_MESSAGE = "Missing query parameter. Required parameters: %s";
    /** Message template for missing request body. */
    public static final String MISSING_REQUEST_BODY_MESSAGE = "Missing request body of type %s";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final int UNPROCESSABLE_CONTENT = 422;
    private final Monitor monitor;

    private final PolicyService policyService;
    private final Negotiator negotiator;
    private final PullTransferInitiator pullTransferInitiator;

    public DspClientController(Monitor monitor,
                               PolicyService policyService,
                               Negotiator negotiator,
                               PullTransferInitiator pullTransferInitiator) {
        this.monitor = monitor;
        this.policyService = policyService;
        this.negotiator = negotiator;
        this.pullTransferInitiator = pullTransferInitiator;
    }


    /**
     * Get policy for asset ID at counter-party, negotiate contract, start data transfer and return access information.
     *
     * @param pullRequestDTO DTO containing all necessary counter-party information.
     */
    @POST
    @Path(ApiConstants.NEGOTIATE)
    public JsonNode negotiateContract(@QueryParam("startTransfer") Boolean startTransfer, PullRequestDTO pullRequestDTO) {
        monitor.debug("POST /%s".formatted(ApiConstants.NEGOTIATE));
        boolean transfer = ofNullable(startTransfer).orElse(true);
        ServiceResult<ContractOffer> contractOfferResult = policyService.getDataset(pullRequestDTO.participantContextId(),
                pullRequestDTO.counterPartyId(),
                pullRequestDTO.counterPartyAddress(), pullRequestDTO.assetId());

        if (contractOfferResult.failed()) {
            monitor.severe("Getting policies failed for provider %s and asset %s: %s".formatted(
                    pullRequestDTO.counterPartyId(), pullRequestDTO.assetId(), contractOfferResult.getFailureDetail()));
            throw new InvalidRequestException(contractOfferResult.getFailureDetail());
        }

        var contractRequest = ContractRequest.Builder.newInstance()
                .protocol(DATASPACE_PROTOCOL_HTTP_V_2025_1)
                .counterPartyAddress(pullRequestDTO.counterPartyAddress())
                .contractOffer(contractOfferResult.getContent())
                .build();

        ServiceResult<ContractAgreement> agreementResult = negotiator.negotiate(pullRequestDTO.participantContextId(), contractRequest);

        if (agreementResult.failed()) {
            monitor.severe("Negotiation failed for provider %s and contractOffer %s: %s".formatted(
                    pullRequestDTO.counterPartyAddress(), contractOfferResult.getContent().getId(), agreementResult.getFailureDetail()));
            throw new InvalidRequestException(agreementResult.getFailureDetail());
        }

        if (!transfer) {
            return toJson(agreementResult.getContent());
        }

        StatusResult<DataAddress> transferResult = pullTransferInitiator.startTransferProcess(pullRequestDTO.participantContextId(), agreementResult.getContent().getId());
        if (transferResult.succeeded()) {
            return toJson(transferResult.getContent());
        }

        throw new WebApplicationException(
                Response.status(UNPROCESSABLE_CONTENT)
                        .entity(transferResult.getFailureMessages().get(transferResult.getFailureMessages().size() - 1))
                        .type(MediaType.TEXT_PLAIN)
                        .build());
    }


    private JsonNode toJson(Object object) {
        return new ObjectMapper().convertValue(object, JsonNode.class);
    }
}
