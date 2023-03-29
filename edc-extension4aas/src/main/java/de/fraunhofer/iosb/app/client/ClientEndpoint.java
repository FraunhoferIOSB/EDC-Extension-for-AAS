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

import static java.lang.String.format;

import java.net.URI;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.types.agreement.ContractAgreement;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.spi.types.domain.HttpDataAddress;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.authentication.CustomAuthenticationRequestFilter;
import de.fraunhofer.iosb.app.client.contract.ContractOfferService;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferObservable;
import de.fraunhofer.iosb.app.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.app.client.negotiation.Negotiator;
import de.fraunhofer.iosb.app.client.storage.AgreementStore;
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

    private static final String ACCEPTED_CONTRACT_OFFERS_PATH = "acceptedContractOffers";
    private static final String CONTRACT_OFFERS_PATH = "contractOffers";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";
    private static final String TRANSFER_PATH = "transfer";

    private static final Logger LOGGER = Logger.getInstance();

    private final Negotiator negotiator;
    private final TransferInitiator transferInitiator;
    private final ContractOfferService contractOfferService;
    private final AgreementStore agreementStore;

    /**
     * Initialize a client endpoint.
     * 
     * @param ownUri                        Needed for providing this connector's
     *                                      address in a data transfer process.
     * @param catalogService                Fetch catalogs from a provider
     *                                      connector.
     * @param consumerNegotiationManager    Initiate a contract negotiation as a
     *                                      consumer.
     * @param contractNegotiationObservable Listen for contract negotiation changes
     *                                      (confirmed, failed, ...).
     * @param transferProcessManager        Initiate a data transfer.
     * @param observable                    Status updates for waiting data transfer
     *                                      requestors to avoid busy waiting.
     * @param dataEndpointAuthRequestFilter Creating and passing through custom api
     *                                      keys for each data transfer.
     */
    public ClientEndpoint(URI ownUri, CatalogService catalogService,
            ConsumerContractNegotiationManager consumerNegotiationManager,
            ContractNegotiationObservable contractNegotiationObservable,
            TransferProcessManager transferProcessManager,
            DataTransferObservable observable,
            CustomAuthenticationRequestFilter dataEndpointAuthRequestFilter) {
        this.negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationObservable);
        this.contractOfferService = new ContractOfferService(catalogService);
        this.transferInitiator = new TransferInitiator(ownUri, transferProcessManager, observable,
                dataEndpointAuthRequestFilter);

        this.agreementStore = new AgreementStore();
    }

    /**
     * Negotiate a contract with a provider edc.
     * WARNING: By initiating this request, any contract provided by the provider
     * for the specified asset will be sent as a contract offer unmodified.
     * 
     * @param providerUrl Provider EDC's URL (IDS endpoint)
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

        ContractOffer contractOffer;
        try {
            contractOffer = contractOfferService.getContractForAssetId(providerUrl, assetId);
        } catch (InterruptedException negotiationException) {
            LOGGER.error(format("Getting contractOffers failed for provider %s and asset %s", providerUrl,
                    assetId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        ContractAgreement agreement;
        try {
            agreement = negotiator.negotiate(providerUrl, contractOffer);
            // Store agreement in agrementStore
            agreementStore.addAgreement(providerUrl, assetId, agreement);
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    contractOffer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        return getData(providerUrl, agreement.getId(), assetId, dataDestinationUrl);
    }

    /**
     * Returns all contract offers offered by the given provider for the given
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
            var contractOffers = contractOfferService.getContractsForAssetId(providerUrl, assetId);
            return Response.ok(contractOffers).build();
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
            // Store agreement in agrementStore
            agreementStore.addAgreement(providerUrl, contractOffer.getAsset().getId(), agreement);
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
     *         message.
     */
    @GET
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("agreementId") String agreementId, @QueryParam("assetId") String assetId,
            @QueryParam("dataDestinationUrl") URL dataDestinationUrl) {
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(agreementId, "AgreementId must not be null");
        Objects.requireNonNull(assetId, "AssetId must not be null");

        try {
            CompletableFuture<String> dataFuture;
            if (Objects.isNull(dataDestinationUrl)) {
                dataFuture = transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId);
            } else {
                var sinkAddress = HttpDataAddress.Builder.newInstance()
                        .baseUrl(dataDestinationUrl.toString())
                        .build();
                dataFuture = transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId, sinkAddress);
            }
            var data = transferInitiator.waitForData(dataFuture, agreementId);
            return Response.ok(data).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Getting data failed for provider %s and agreementId %s", providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

    /**
     * Add a contract offer to the 'accepted list'. This contract offer's policies'
     * rules or the rules of any other stored contract offer's policies' rules must
     * be matched on automated contract negotiation. This means, any contract offer
     * by a provider must have the same rules as any of the stored contract offers.
     * 
     * @param contractOffers The contractOffer to add (Only its rules are relevant)
     * 
     * @return OK as response.
     */
    @POST
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response addAcceptedContractOffers(ContractOffer[] contractOffers) {
        LOGGER.log(format("Adding %s accepted contract offers", contractOffers.length));
        Objects.requireNonNull(contractOffers, "ContractOffer is null");
        contractOfferService.addAccepted(contractOffers);
        return Response.ok().build();
    }

    /**
     * Returns all contract offers in the 'accepted list'.
     * 
     * @return A list of accepted contract offers
     */
    @GET
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response getAcceptedContractOffers() {
        LOGGER.log("Returning accepted contract offers");
        return Response.ok(contractOfferService.getAccepted()).build();
    }

    /**
     * Removes a contract offer from the 'accepted list'.
     * 
     * @param contractOffer The id of the contractOffer to remove
     * @return OK as response.
     */
    @DELETE
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response deleteAcceptedContractOffer(@QueryParam("contractOfferId") String contractOfferId) {
        LOGGER.log(format("Removing contract offer with id %s", contractOfferId));
        Objects.requireNonNull(contractOfferId, "ContractOffer is null");
        contractOfferService.removeAccepted(contractOfferId);
        return Response.ok().build();
    }

    /**
     * Updates a contract offer of the 'accepted list'.
     * 
     * @param contractOffer The contractOffer to update
     * @return OK as response.
     */
    @PUT
    @Path(ACCEPTED_CONTRACT_OFFERS_PATH)
    public Response updateAcceptedContractOffer(ContractOffer contractOffer) {
        LOGGER.log(format("Updating contract offer with id %s", contractOffer.getId()));
        Objects.requireNonNull(contractOffer, "contractOffer is null");
        contractOfferService.updateAccepted(contractOffer.getId(), contractOffer);
        return Response.ok().build();
    }

    /**
     * Return agreements of already negotiated contracts.
     * 
     * @param providerUrl Provider whose contractAgreements are to be fetched
     *                    (Optional)
     * @param assetId     Asset ID for single agremeent (Optional)
     * @return Response containing agreements in a list (Provider, AssetId,
     *         AgreementId)
     */
    @GET
    @Path("agreements")
    public Response getAgreements(@QueryParam("providerUrl") URL providerUrl, @QueryParam("assetId") String assetId) {
        if (Objects.nonNull(assetId) && Objects.nonNull(providerUrl)) {
            LOGGER.log(format("Returning agreements for provider %s and asset %s", providerUrl, assetId));
            return Response.ok(agreementStore.getAgreement(providerUrl, assetId)).build();
        } else if (Objects.nonNull(providerUrl)) {

            LOGGER.log(format("Returning agreements for provider %s", providerUrl));
            return Response.ok(agreementStore.getAllProviderAgreements(providerUrl)).build();
        } else {
            LOGGER.log("Returning agreements");
            return Response.ok(agreementStore.getAllAgreements()).build();
        }
    }

    /**
     * Remove an agreement from the extension's agreementStore
     * 
     * @param agreementId AgreementId of agreement to be removed (non null)
     * @return Status of the remove operation
     */
    @DELETE
    @Path("agreements")
    public Response deleteAgreement(@QueryParam("agremeentId") String agreementId) {
        Objects.requireNonNull(agreementId, "agreementId cannot be null");
        LOGGER.log(format("Removing agreement with id %s", agreementId));
        if (agreementStore.removeAgreement(agreementId)) {
            return Response.ok().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
