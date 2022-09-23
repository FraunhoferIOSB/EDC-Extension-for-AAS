package de.fraunhofer.iosb.app.controller;

import de.fraunhofer.iosb.app.RequestType;
import jakarta.ws.rs.core.Response;

import java.net.URL;

/**
 * A controller interface.
 */
public interface Controllable {

    /**
     * Handles a request of type {@link RequestType}.

     * @param requestType The request type
     * @param requestData String array containing the request body as first and
     *                    request parameters as second element.
     * @return A response object according to {@link jakarta.ws.rs.core.Response
     *         Response}
     */
    Response handleRequest(RequestType requestType, URL url, String... requestData);

}
