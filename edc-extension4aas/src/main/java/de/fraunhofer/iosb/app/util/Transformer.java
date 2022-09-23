package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.app.Logger;
import jakarta.ws.rs.core.Response;

import java.io.IOException;

/**
 * Util; Transform similar objects into other frameworks' implementations
 */
public final class Transformer {

    private static final Logger LOGGER = Logger.getInstance();

    private Transformer() {
    }

    /*
     * Transform an okHttp client response to a jakarta response object.
     */
    public static Response okHttpResponseToJakartaResponse(okhttp3.Response response) {
        int statuscode = response.code();
        String body;
        try {
            body = response.body().string();
        } catch (IOException e) {
            LOGGER.error("Failed transforming HTTP Response", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.status(statuscode).entity(body).build();
    }
}
