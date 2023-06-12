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
package de.fraunhofer.iosb.app.client;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.client.contract.ContractOfferService;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferObservable;
import de.fraunhofer.iosb.app.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.app.client.negotiation.Negotiator;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

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

    private static final String ACCEPTED_CONTRACT_OFFERS_PATH = "acceptedContractOffers";
    private static final String CONTRACT_OFFERS_PATH = "contractOffers";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";
    private static final String TRANSFER_PATH = "transfer";

    private static final Logger LOGGER = Logger.getInstance();

    private final Negotiator negotiator;
    private final TransferInitiator transferInitiator;
    private final ContractOfferService contractOfferService;

    /**
     * Initialize a client endpoint.
     *
     * @param ownUri                        Needed for providing this connector's
     *                                      address in a data transfer process.
     * @param catalogService                Fetch catalogs from a provider
     *                                      connector.
     * @param consumerNegotiationManager    Initiate a contract negotiation as a
     *                                      consumer.
     * @param contractNegotiationStore      Check negotiation status.
     * @param contractNegotiationObservable Listen for contract negotiation changes
     *                                      (confirmed, failed, ...).
     * @param transferProcessManager        Initiate a data transfer.
     * @param observable                    Status updates for waiting data transfer
     *                                      requesters to avoid busy waiting.
     * @param dataEndpointAuthRequestFilter Creating and passing through custom api
     *                                      keys for each data transfer.
     */
    public ClientEndpoint(URI ownUri, CatalogService catalogService,
                          ConsumerContractNegotiationManager consumerNegotiationManager,
                          ContractNegotiationStore contractNegotiationStore,
                          ContractNegotiationObservable contractNegotiationObservable,
                          TransferProcessManager transferProcessManager,
                          DataTransferObservable observable,
                          CustomAuthenticationRequestFilter dataEndpointAuthRequestFilter) {
        this.negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationObservable,
                contractNegotiationStore);
        this.contractOfferService = new ContractOfferService(catalogService);
        this.transferInitiator = new TransferInitiator(ownUri, transferProcessManager, observable,
                dataEndpointAuthRequestFilter);
    }

    /**
     * Negotiate a contract with a provider edc.
     * WARNING: By initiating this request, any contract provided by the provider
     * for the specified asset will be sent as a contract offer unmodified.
     *
     * @param providerUrl Provider EDCs URL (IDS endpoint)
     * @param assetId     ID of the asset to be retrieved
     * @return Asset data
     */
    @POST
    @Path(NEGOTIATE_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL providerUrl,
                                      @QueryParam("assetId") String assetId, @QueryParam("dataDestinationUrl") URL dataDestinationUrl) {
        LOGGER.debug(format("Received a %s POST request", NEGOTIATE_PATH));
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(assetId, "Asset ID must not be null");

        Policy policy;
        try {
            policy = contractOfferService.getAcceptablePolicyForAssetId(providerUrl, assetId);
        } catch (InterruptedException negotiationException) {
            LOGGER.error(format("Getting contractOffers failed for provider %s and asset %s", providerUrl,
                    assetId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        ContractOffer offer = ContractOffer.Builder.newInstance()
                .id("contract-offer-" + assetId + "-" + policy.hashCode())
                .policy(policy)
                .assetId(assetId)
                .providerId(providerUrl.toString())
                .build();
        ContractAgreement agreement;

        try {
            agreement = negotiator.negotiate(providerUrl, offer);
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    offer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        return getData(providerUrl, agreement.getId(), assetId, dataDestinationUrl);
    }

    /**
     * Returns Datasets offered by the given provider for the given
     * assetID.
     *
     * @param providerUrl Provider whose contracts should be fetched (non null).
     * @param assetId     Asset ID for which contractOffers should be fetched.
     * @return A list of contract offers or an error message.
     */
    @GET
    @Path(CONTRACT_OFFERS_PATH)
    public Response getContractOffers(@QueryParam("providerUrl") URL providerUrl,
                                      @QueryParam("assetId") String assetId) {
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");

        try {
            var datasets = contractOfferService.getDatasetsForAssetId(providerUrl, assetId);
            return Response.ok(datasets).build();
        } catch (InterruptedException interruptedException) {
            LOGGER.error(format("Getting contractOffers failed for provider %s and asset %s", providerUrl,
                    assetId), interruptedException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(interruptedException.getMessage())
                    .build();
        }
    }

    /**
     * Initiate a contract negotiation, acting as a consumer, with a provider
     * connector.
     *
     * @param providerUrl   The provider's url.
     * @param contractOffer A contract offer to be negotiated with.
     * @return An agreementID on success or an error message on error.
     */
    @POST
    @Path(NEGOTIATE_CONTRACT_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL providerUrl,
                                      ContractOffer contractOffer) {
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(contractOffer, "ContractOffer must not be null");
        try {
            var agreement = negotiator.negotiate(providerUrl, contractOffer);
            return Response.ok(agreement).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    contractOffer.getId()), negotiationException);
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
     * message.
     */
    @GET
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
                            @QueryParam("agreementId") String agreementId, @QueryParam("assetId") String assetId,
                            @QueryParam("dataDestinationUrl") URL dataDestinationUrl) {
        Objects.requireNonNull(providerUrl, "providerUrl must not be null");
        Objects.requireNonNull(agreementId, "agreementId must not be null");
        Objects.requireNonNull(assetId, "assetId must not be null");

        if (Objects.isNull(dataDestinationUrl)) {
            try {
                var dataFuture = transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId);
                var data = transferInitiator.waitForData(dataFuture, agreementId);
                return Response.ok(data).build();

            } catch (InterruptedException | ExecutionException negotiationException) {
                LOGGER.error(format("Getting data failed for provider %s and agreementId %s", providerUrl,
                        agreementId), negotiationException);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                        .build();
            }
        } else {
            var sinkAddress = HttpDataAddress.Builder.newInstance()
                    .baseUrl(dataDestinationUrl.toString())
                    .build();
            // Don't need future as the EDC does not receive the data
            transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId, sinkAddress);
            return Response.ok(format("Data transfer request to URL %s sent.", dataDestinationUrl)).build();
        }
    }

    /**
     * Add policyDefinitions to the 'accepted list'. These policies or any other stored
     * policy must be matched on automated contract negotiation.
     * This means, any policyDefinition by a provider must have the same rules
     * as any of the stored policyDefinitions.
     *
     * @param policyDefinitions The policyDefinitions to add (Only their rules are relevant)
     * @return OK as response.
     */
    @POST
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response addAcceptedContractOffers(PolicyDefinition[] policyDefinitions) {
        LOGGER.log(format("Adding %s accepted contract offers", policyDefinitions.length));
        Objects.requireNonNull(policyDefinitions, "ContractOffer is null");
        contractOfferService.addAccepted(policyDefinitions);
        return Response.ok().build();
    }

    /**
     * Returns all policyDefinitions in the 'accepted list'.
     *
     * @return A list of accepted policyDefinitions
     */
    @GET
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response getAcceptedPolicyDefinitions() {
        LOGGER.log("Returning accepted policyDefinitions");
        return Response.ok(contractOfferService.getAccepted()).build();
    }

    /**
     * Removes a policyDefinition from the 'accepted list'.
     *
     * @param policyDefinitionId The id of the policyDefinition to remove
     * @return OK as response.
     */
    @DELETE
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response deleteAcceptedContractOffer(@QueryParam("contractOfferId") String policyDefinitionId) {
        LOGGER.log(format("Removing policyDefinition with id %s", policyDefinitionId));
        Objects.requireNonNull(policyDefinitionId, "PolicyDefinition ID is null");
        contractOfferService.removeAccepted(policyDefinitionId);
        return Response.ok().build();
    }

    /**
     * Updates a policyDefinition of the 'accepted list'.
     *
     * @param policyDefinition The policyDefinition to update
     * @return OK as response.
     */
    @PUT
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response updateAcceptedContractOffer(PolicyDefinition policyDefinition) {
        LOGGER.log(format("Updating policyDefinition with id %s", policyDefinition.getId()));
        Objects.requireNonNull(policyDefinition, "policyDefinition is null");
        contractOfferService.updateAccepted(policyDefinition.getId(), policyDefinition);
        return Response.ok().build();
    }
}
