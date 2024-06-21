package de.fraunhofer.iosb.aas;

import de.fraunhofer.iosb.dataplane.aas.spi.AasDataAddress;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AasReader {

    private final OkHttpClient client;

    AasReader(OkHttpClient okHttpClient) {
        this.client = okHttpClient;
    }

    public Response get(AasDataAddress aasDataAddress) throws IOException {
        var request = new Request.Builder()
                .get()
                .url(HttpUrl.get(aasDataAddress.getBaseUrl())
                        .newBuilder()
                        .addPathSegment(aasDataAddress.referenceChainAsPath())
                        .build())
                .headers(Headers.of(aasDataAddress.getAdditionalHeaders()))
                .build();

        return client.newCall(request).execute();
    }

}
