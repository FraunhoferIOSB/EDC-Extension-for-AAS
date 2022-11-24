package de.fraunhofer.iosb.app.client.dataTransfer;

import static java.lang.String.format;

import java.util.Objects;

import de.fraunhofer.iosb.app.Logger;
import de.fraunhofer.iosb.app.client.ClientEndpoint;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint for automated data transfer
 */
@Consumes({ MediaType.APPLICATION_JSON, MediaType.WILDCARD })
@Produces({ MediaType.APPLICATION_JSON })
@Path(ClientEndpoint.AUTOMATED_PATH)
public class DataTransferEndpoint {

    /*
     * Path for providers to send data to.
     */
    public static final String RECEIVE_DATA_PATH = "receiveData";

    private static final Logger LOGGER = Logger.getInstance();
    private final DataTransferObservable observable;

    public DataTransferEndpoint(DataTransferObservable observable) {
        this.observable = observable;
    }

    /**
     * Receive data of an agreement.
     * 
     * @param agreementId The agreement ID corresponding to the data in the request
     *                    body.
     * @param requestBody The asset data of a transfer request.
     * @return OK as response.
     */
    @POST
    @Path(RECEIVE_DATA_PATH)
    public Response receiveData(@QueryParam("agreementId") String agreementId, String requestBody) {
        LOGGER.log(format("Receiving data for agreement %s...", agreementId));
        Objects.requireNonNull(agreementId);
        Objects.requireNonNull(requestBody);
        observable.update(agreementId, requestBody);
        return Response.ok().build();
    }

}
