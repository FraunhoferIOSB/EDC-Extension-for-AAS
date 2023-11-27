package de.fraunhofer.iosb.client;

import static java.lang.String.format;
import static org.eclipse.edc.protocol.dsp.spi.types.HttpMessageProtocol.DATASPACE_PROTOCOL_HTTP;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.dataplane.http.spi.HttpDataAddress;
import org.eclipse.edc.connector.policy.spi.PolicyDefinition;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.agreement.ContractAgreement;
import org.eclipse.edc.spi.types.domain.offer.ContractOffer;

import de.fraunhofer.iosb.client.dataTransfer.TransferInitiator;
import de.fraunhofer.iosb.client.negotiation.Negotiator;
import de.fraunhofer.iosb.client.policy.PolicyService;
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

    private final Negotiator negotiator;
    private final PolicyService policyService;
    private final TransferInitiator transferInitiator;

    /**
     * Initialize a client endpoint.
     *
     * @param policyService     Finds out policy for a given asset id and provider
     *                          EDC url.
     * @param negotiator        Send contract offer, negotiation status watch.
     * @param transferInitiator Initiate transfer requests.
     */
    public ClientEndpoint(Monitor monitor, Negotiator negotiator, PolicyService policyService,
            TransferInitiator transferInitiator) {
        this.monitor = monitor;

        this.negotiator = negotiator;
        this.policyService = policyService;
        this.transferInitiator = transferInitiator;
    }

    /**
     * Negotiate a contract with a provider edc.
     * WARNING: By initiating this request, any policy provided by the provider for
     * the specified asset will be sent
     * as a contract offer unmodified if edc.aas.client.acceptAllProviderOffers is
     * set to true.
     *
     * @param providerUrl Provider EDCs URL (IDS endpoint)
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
            idPolicyPair = policyService.getAcceptablePolicyForAssetId(providerUrl, assetId);
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
            agreement = negotiator.negotiate(contractRequest);
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(format("[Client] Negotiation failed for provider %s and contractOffer %s", providerUrl,
                    offer.getId()), negotiationException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(negotiationException.getMessage())
                    .build();
        }

        return getData(providerUrl, agreement.getId(), assetId, dataDestinationUrl);
    }

    /**
     * Returns dataset offered by the given provider for the given asset.
     *
     * @param providerUrl Provider whose dataset should be fetched (non null).
     * @param assetId     Asset ID for which dataset should be fetched.
     * @return A dataset or an error message.
     */
    @GET
    @Path(DATASET_PATH)
    public Response getDataset(@QueryParam("providerUrl") URL providerUrl,
            @QueryParam("assetId") String assetId) {
        if (Objects.isNull(providerUrl)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Provider URL must not be null").build();
        }

        try {
            var datasets = policyService.getDatasetForAssetId(providerUrl, assetId);
            return Response.ok(datasets).build();
        } catch (InterruptedException interruptedException) {
            monitor.severe(format("[Client] Getting datasets failed for provider %s and asset %s", providerUrl,
                    assetId), interruptedException);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(interruptedException.getMessage())
                    .build();
        }
    }

    /**
     * Initiate a contract negotiation, acting as a consumer, with a provider
     * connector.
     *
     * @param contractRequest The contract request to be sent.
     * @return An agreementID on success or an error message on error.
     */
    @POST
    @Path(NEGOTIATE_CONTRACT_PATH)
    public Response negotiateContract(ContractRequest contractRequest) {
        Objects.requireNonNull(contractRequest, "ContractRequest must not be null");
        try {
            var agreement = negotiator.negotiate(contractRequest);
            return Response.ok(agreement).build();
        } catch (InterruptedException | ExecutionException negotiationException) {
            monitor.severe(
                    format("[Client] Negotiation failed for provider %s and contractRequest %s", contractRequest.getProviderId(),
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
        Objects.requireNonNull(providerUrl, "providerUrl must not be null");
        Objects.requireNonNull(agreementId, "agreementId must not be null");
        Objects.requireNonNull(assetId, "assetId must not be null");

        if (Objects.isNull(dataDestinationUrl)) {
            try {
                var dataFuture = transferInitiator.initiateTransferProcess(providerUrl, agreementId, assetId);
                var data = transferInitiator.waitForData(dataFuture, agreementId);
                return Response.ok(data).build();

            } catch (InterruptedException | ExecutionException negotiationException) {
                monitor.severe(format("[Client] Getting data failed for provider %s and agreementId %s", providerUrl,
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
     * Add policyDefinitions to the 'accepted list'. These policies or any other
     * stored
     * policy must be matched on automated contract negotiation.
     * This means, any policyDefinition by a provider must have the same rules
     * as any of the stored policyDefinitions.
     *
     * @param policyDefinitions The policyDefinitions to add (Only their rules are
     *                          relevant)
     * @return OK as response.
     */
    @POST
    @Path(ACCEPTED_POLICIES_PATH)
    public Response addAcceptedPolicies(PolicyDefinition[] policyDefinitions) {
        if (Objects.isNull(policyDefinitions)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinitions array").build();
        }
        monitor.info(format("[Client] Adding %s accepted contract offers", policyDefinitions.length));

        policyService.addAccepted(policyDefinitions);
        return Response.ok().build();
    }

    /**
     * Returns all policyDefinitions in the 'accepted list'.
     *
     * @return A list of accepted policyDefinitions
     */
    @GET
    @Path(ACCEPTED_POLICIES_PATH)
    public Response getAcceptedPolicyDefinitions() {
        monitor.info("[Client] Returning accepted policyDefinitions");
        return Response.ok(policyService.getAccepted()).build();
    }

    /**
     * Removes a policyDefinition from the 'accepted list'.
     *
     * @param policyDefinitionId The id of the policyDefinition to remove
     * @return OK as response.
     */
    @DELETE
    @Path(ACCEPTED_POLICIES_PATH)
    public Response deleteAcceptedPolicyDefinition(@QueryParam("policyDefinitionId") String policyDefinitionId) {
        monitor.info(format("[Client] Removing policyDefinition with id %s", policyDefinitionId));
        if (Objects.isNull(policyDefinitionId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinitionId parameter").build();
        }
        var removed = policyService.removeAccepted(policyDefinitionId);

        if (removed.isPresent()) {
            return Response.ok(policyDefinitionId).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

    /**
     * Updates a policyDefinition of the 'accepted list'.
     *
     * @param policyDefinition The policyDefinition to update
     * @return OK as response.
     */
    @PUT
    @Path(ACCEPTED_POLICIES_PATH)
    public Response updateAcceptedPolicyDefinition(PolicyDefinition policyDefinition) {
        if (Objects.isNull(policyDefinition)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Missing policyDefinition").build();
        }
        monitor.info(format("[Client] Updating policyDefinition with id %s", policyDefinition.getId()));

        var updated = policyService.updateAccepted(policyDefinition.getId(), policyDefinition);
        if (updated.isPresent()) {
            return Response.ok(policyDefinition.getId()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).entity("Unknown policyDefinitionId.").build();
    }

}
