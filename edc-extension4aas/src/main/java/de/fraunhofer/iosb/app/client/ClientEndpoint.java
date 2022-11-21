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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.edc.connector.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.contract.spi.negotiation.observe.ContractNegotiationObservable;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.spi.catalog.CatalogService;
import org.eclipse.edc.connector.transfer.spi.TransferProcessManager;
import org.eclipse.edc.spi.EdcException;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.client.contract.ContractOfferService;
import de.fraunhofer.iosb.app.client.contract.ContractOfferStore;
import de.fraunhofer.iosb.app.client.dataTransfer.DataTransferObservable;
import de.fraunhofer.iosb.app.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.app.client.negotiation.Negotiator;
import de.fraunhofer.iosb.app.model.configuration.Configuration;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
    /*
     * Path for providers to send data to.
     */
    public static final String RECEIVE_DATA_PATH = "receiveData";
    private static final String CONTRACT_OFFER_PATH = "contractOffer";
    private static final String CONTRACT_OFFERS_PATH = "contractOffers";
    private static final String NEGOTIATE_CONTRACT_PATH = "negotiateContract";
    private static final String NEGOTIATE_PATH = "negotiate";
    private static final String TRANSFER_PATH = "transfer";

    private static final Logger LOGGER = Logger.getInstance();

    private final Negotiator negotiator;
    private final TransferInitiator transferInitiator;
    private final ContractOfferService contractOfferService;
    private final DataTransferObservable observable;

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
     */
    public ClientEndpoint(URI ownUri, CatalogService catalogService,
            ConsumerContractNegotiationManager consumerNegotiationManager,
            ContractNegotiationObservable contractNegotiationObservable,
            TransferProcessManager transferProcessManager) {
        this.negotiator = new Negotiator(consumerNegotiationManager, contractNegotiationObservable);
        this.contractOfferService = new ContractOfferService(catalogService, new ContractOfferStore());
        this.transferInitiator = new TransferInitiator(ownUri, transferProcessManager);

        observable = new DataTransferObservable();
    }

    /**
     * Negotiate a contract with a provider edc.
     * WARNING: By initiating this request, any contract provided by the provider
     * for the specified asset will be sent as a contract offer unmodified.
     * 
     * @param providerUrl Provider EDC's URL (IDS endpoint)
     * @param assetId     ID of the asset to be retrieved
     * @return Asset data
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @POST
    @Path(NEGOTIATE_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("assetId") String assetId) {
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

        String agreementId;
        try {
            agreementId = negotiator.negotiate(providerUrl, contractOffer);
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    contractOffer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        try {
            var data = waitForData(providerUrl, agreementId, assetId);
            return Response.ok(data).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Getting data failed for provider %s and agreementId %s", providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

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

    @POST
    @Path(NEGOTIATE_CONTRACT_PATH)
    public Response negotiateContract(@QueryParam("providerUrl") URL providerUrl,
            ContractOffer contractOffer) {
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(contractOffer, "ContractOffer must not be null");
        try {
            var agreementId = negotiator.negotiate(providerUrl, contractOffer);
            return Response.ok(agreementId).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    contractOffer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

    @GET
    @Path(TRANSFER_PATH)
    public Response getData(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("agreementId") String agreementId, @QueryParam("assetId") String assetId) {
        Objects.requireNonNull(providerUrl, "Provider URL must not be null");
        Objects.requireNonNull(agreementId, "AgreementId must not be null");
        Objects.requireNonNull(assetId, "AssetId must not be null");
        try {
            var data = waitForData(providerUrl, agreementId, assetId);
            return Response.ok(data).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            LOGGER.error(format("Getting data failed for provider %s and agreementId %s", providerUrl,
                    agreementId), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }
    }

    /**
     * Only for automated access. Send data of an agreement to this endpoint.
     * 
     * 
     * @param agreementId The agreement ID corresponding to the data in the request
     *                    body.
     * @param requestBody The asset data of a transfer request.
     * @return OK as response.
     */
    @POST
    @Path(RECEIVE_DATA_PATH)
    public Response putData(@QueryParam("agreementId") String agreementId, String requestBody) {
        LOGGER.log(format("Receiving data for agreement %s...", agreementId));
        Objects.requireNonNull(agreementId);
        Objects.requireNonNull(requestBody);
        observable.update(agreementId, requestBody);
        return Response.ok().build();
    }

    @POST
    @Path(CONTRACT_OFFER_PATH)
    public Response addAcceptedContractOffer(ContractOffer contractOffer) {
        LOGGER.log("Received new accepted contract offer");
        Objects.requireNonNull(contractOffer, "ContractOffer is null");
        contractOfferService.addAccepted(contractOffer);
        return Response.ok().build();
    }

    private String waitForData(URL providerUrl, String agreementId, String assetId)
            throws InterruptedException, ExecutionException {
        // Prepare for incoming data
        var dataFuture = new CompletableFuture<String>();
        observable.register(dataFuture, agreementId);
        transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId);
        try {
            // Fetch everytime to adapt to runtime config changes
            var transferTimeout = Configuration.getInstance().getWaitForTransferTimeout();
            return dataFuture.get(transferTimeout, TimeUnit.SECONDS);
        } catch (TimeoutException transferTimeoutExceededException) {
            observable.unregister(agreementId);
            throw new EdcException(format("Waiting for an transfer failed for agreementId: %s", agreementId),
                    transferTimeoutExceededException);
        }
    }
}
