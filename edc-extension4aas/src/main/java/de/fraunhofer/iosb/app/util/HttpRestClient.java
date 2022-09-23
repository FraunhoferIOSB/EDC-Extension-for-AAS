package de.fraunhofer.iosb.app.util;

import de.fraunhofer.iosb.app.Logger;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

public class HttpRestClient {

    private final Logger logger;
    private final OkHttpClient client;

    public HttpRestClient(OkHttpClient client) {
        logger = Logger.getInstance();
        this.client = client;
    }

    /**
     * Issue a get request to a given url

     * @param url the url to where the get request goes
     * @return Response by the service behind the url
     */
    public Response get(URL url) throws IOException {
        logger.debug("GET " + url);
        var request = new Request.Builder()
                .url(HttpUrl.get(url))
                .get()
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a put request to a given url

     * @param url     the url to where the put request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response put(URL url, String payload) throws IOException {
        logger.debug("PUT " + url);
        var request = new Request.Builder()
                .url(HttpUrl.get(url))
                .put(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a post request to a given url

     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response post(URL url, String payload) throws IOException {
        logger.debug("POST " + url);
        var request = new Request.Builder()
                .url(HttpUrl.get(url))
                .post(RequestBody.create(payload, MediaType.parse("application/json")))
                .build();
        return client.newCall(request).execute();
    }

    /**
     * Issue a delete request to a given url

     * @param url     the url to where the post request goes
     * @param payload payload of this operation
     * @return Response by the service behind the url
     */
    public Response delete(URL url, String payload) throws IOException {
        logger.debug("DELETE " + url);

        Request request;
        if (Objects.nonNull(payload)) {
            RequestBody requestBody = RequestBody.create(payload, MediaType.parse("application/json"));
            request = new Request.Builder()
                    .url(HttpUrl.get(url))
                    .delete(requestBody)
                    .build();
        } else {
            request = new Request.Builder()
                    .url(HttpUrl.get(url))
                    .delete()
                    .build();
        }
        return client.newCall(request).execute();
    }

    /**
     * Build jakarta reponse from okHttp response object

     * @param toTransform okHttp response
     */
    public jakarta.ws.rs.core.Response toJakartaResponse(Response toTransform) {
        int statuscode = toTransform.code();
        String body;
        try {
            body = toTransform.body().string();
        } catch (IOException e) {
            logger.error("Failed transforming HTTP Response", e);
            return jakarta.ws.rs.core.Response.status(jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return jakarta.ws.rs.core.Response.status(statuscode).entity(body).build();
    }
}
